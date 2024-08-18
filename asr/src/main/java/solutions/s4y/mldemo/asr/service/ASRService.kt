package solutions.s4y.mldemo.asr.service

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.newSingleThreadContext
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.firebase.FirebaseBlob
import solutions.s4y.mldemo.asr.service.accumulator.GrowingAccumulator
import solutions.s4y.mldemo.asr.service.whisper.WhisperInferrer
import solutions.s4y.mldemo.asr.service.whisper.WhisperPipeline
import solutions.s4y.mldemo.asr.service.whisper.WhisperTFLogMel
import solutions.s4y.mldemo.asr.service.whisper.WhisperTokenizer
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import java.io.Closeable
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ASRService @Inject constructor(@ApplicationContext context: Context) :
    Closeable {
    enum class State {
        IDLE,
        NON_SPEECH,
        SPEECH
    }

    companion object {
        private const val TAG = "ASRService"
        private const val NON_SPEECH_THRESHOLD = 2
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val inferenceContext = newSingleThreadContext("inference_asr")
    private val _flow: MutableSharedFlow<String> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _flowState: MutableStateFlow<State> =
        MutableStateFlow(State.IDLE)
    private val _flowModelReady: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    private val growingAccumulator = GrowingAccumulator()
    private var jobAccumulator: Job? = null
    private var jobWhisper: Job? = null
    private var localModelPath = File("")
    private val melSpectrogram: IMelSpectrogram = WhisperTFLogMel(context, inferenceContext)
    private val nonSpeechCount = AtomicInteger(0)
    private lateinit var tryEmitWaveForms: (FloatArray) -> Boolean
    private lateinit var closeWaveForms: () -> Unit
    private val waveForms: Flow<FloatArray> = callbackFlow {
        tryEmitWaveForms = { trySend(it).isSuccess }
        closeWaveForms = { close() }
        awaitClose { }
    }
    private var whisperPipeline = AtomicReference<WhisperPipeline?>(null)

    val flow: SharedFlow<String> = _flow
    val flowState: StateFlow<State> = _flowState
    val flowModelReady: StateFlow<Boolean> = _flowModelReady

    private suspend fun isMinDuration() = growingAccumulator.duration() > 2
    private suspend fun isMaxDuration() = growingAccumulator.duration() > 20

    override fun close() {
        closeWaveForms()
        jobAccumulator?.cancel()
        jobWhisper?.cancel()
        Log.i(TAG, "ASR: IDLE")
        _flowState.value = State.IDLE
    }

    fun start(classesFlow: Flow<IVoiceClassifier.Classes>, feedingScope: CoroutineScope) {
        val whisperProvider =
            whisperPipeline.get() ?: throw IllegalStateException("Model is not ready")
        _flowState.value = State.NON_SPEECH
        jobAccumulator = classesFlow
            // side effect: grow accumulator, for sake of performance
            .onEach {
                val cls = if (it.ids.isEmpty()) -1 else it.ids[0]
                // Speech
                // Child speech, kid speaking
                // Narration, monologue
                // * Music
                // Singing
                if (cls == 0 || cls == 1 || cls == 3 || /*cls == 24 ||*/ cls == 132) {
                    nonSpeechCount.set(0)
                    growingAccumulator.growAccumulator(it.waveForms)
                    _flowState.value = State.SPEECH
                    Log.d(TAG, "Speech cls=$cls detected for ${growingAccumulator.duration()}s, emit = ${isMaxDuration()}")
                    // continued speech should not be transcribed immediately
                    // wait either for specific duration or for silence
                    if (isMaxDuration()) {
                        val success = tryEmitWaveForms(growingAccumulator.waveForms())
                        Log.d(TAG, "emit waveforms: $success")
                    }
                    return@onEach
                }
                _flowState.value = State.NON_SPEECH
                val count = nonSpeechCount.incrementAndGet()
                @Suppress("KotlinConstantConditions")
                when {
                    count < NON_SPEECH_THRESHOLD -> {
                        // skip short pause
                        Log.d(
                            TAG,
                            "Non speech cls=$cls detected $count times (short pause) , accumulated=${growingAccumulator.duration()}s"
                        )
                    }

                    count == NON_SPEECH_THRESHOLD -> {
                        Log.d(
                            TAG,
                            "Non speech cls=$cls detected $count times, (emit = ${isMinDuration()}), accumulated=${growingAccumulator.duration()}s"
                        )
                        if (isMinDuration()) {
                            val success = tryEmitWaveForms(growingAccumulator.waveForms())
                            Log.d(TAG, "emit waveforms: $success")
                        }
                        growingAccumulator.reset()
                    }

                    count > NON_SPEECH_THRESHOLD -> {
                        Log.d(
                            TAG,
                            "Non speech cls=$cls detected $count times, (long pause), accumulated=${growingAccumulator.duration()}s"
                        )
                    }
                }
            }
            .launchIn(feedingScope)
        jobWhisper = whisperProvider.flow
            .onEach {
                Log.d(TAG, "whisper response: $it")
                _flow.tryEmit(it)
            }
            .launchIn(feedingScope)
    }

    suspend fun loadModel(context: Context, gcStoragePath: String) {
        stop()
        localModelPath = File(context.filesDir, gcStoragePath)
        _flowModelReady.value = false
        FirebaseBlob(gcStoragePath, localModelPath).get()
        val provider = WhisperPipeline(
            waveForms,
            melSpectrogram,
            WhisperInferrer(localModelPath, inferenceContext),
            WhisperTokenizer(context),
        )
        whisperPipeline.set(provider)
        _flowModelReady.value = true
    }

    fun stop() {
        jobAccumulator?.cancel()
        jobWhisper?.cancel()
        jobAccumulator = null
        jobWhisper = null
        nonSpeechCount.set(0)
        growingAccumulator.reset()
        _flowState.value = State.IDLE
    }

    suspend fun reset() {
        growingAccumulator.reset()
    }
}