package solutions.s4y.mldemo.asr.service

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.preferences.CurrentModel
import solutions.s4y.mldemo.asr.service.androidspeech.androidSpeechRecognitions
import solutions.s4y.mldemo.asr.service.gcs.gcsEncoderDecoderPath
import solutions.s4y.mldemo.asr.service.gcs.gcsFeaturesExtractorPath
import solutions.s4y.mldemo.asr.service.whisper.EncoderDecoder
import solutions.s4y.mldemo.asr.service.logmel.TFLiteLogMel
import solutions.s4y.mldemo.asr.service.whisper.SpeechState
import solutions.s4y.mldemo.asr.service.whisper.WhisperTokenizer
import solutions.s4y.mldemo.asr.service.whisper.accumulate16000
import solutions.s4y.mldemo.asr.service.whisper.whisper
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import solutions.s4y.tflite.base.TfLiteFactory
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class ASRService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tfLiteFactory: TfLiteFactory,
    val audioService: AudioService,
    val classifier: VoiceClassificationService,
    private val currentModelPreference: CurrentModel = CurrentModel(context)
) : Closeable {
    /***********************************************************************************************
     * Common
     **********************************************************************************************/
    private val mfASR: MutableSharedFlow<String> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val mfIsASRActive: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    private val mfSpeechState: MutableStateFlow<SpeechState> =
        MutableStateFlow(SpeechState.IDLE)
    private val mfIsReady: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private fun Flow<IVoiceClassifier.Classes>.onEachEmitState() = this.onEach { classes ->
        if (classes.isSpeech) {
            mfSpeechState.value = SpeechState.SPEECH
        } else {
            mfSpeechState.value = SpeechState.NON_SPEECH
        }
    }

    var currentModel by currentModelPreference
    val flowASR: SharedFlow<String> = mfASR
    val flowCurrentModel = currentModelPreference.flow
    val flowIsActive: StateFlow<Boolean> = mfIsASRActive
    val flowIsReady: StateFlow<Boolean> = mfIsReady
    val flowSpeechState: StateFlow<SpeechState> = mfSpeechState

    companion object {
        private const val TAG = "ASRService"
    }

    override fun close() {
        stopAndroidSpeechRecognizer()
        stopTfLite()
        freeTFLite()
    }

    @SuppressLint("MissingPermission")
    fun startASR(context: Context, parentScope: CoroutineScope) {
        stopTfLite()
        stopAndroidSpeechRecognizer()
        if (currentModel.isAndroid) {
            startAndroidSpeechRecognizer(
                context,
                parentScope,
                classifier.flowClasses.onEachEmitState()
            )
        } else {
            startTfLite(parentScope, classifier.flowClasses.onEachEmitState())
        }
        audioService.startRecording()
        classifier.start(audioService.samplesFlow, parentScope)
    }

    fun stopASR() {
        stopTfLite()
        stopAndroidSpeechRecognizer()
        audioService.stopRecording()
        classifier.stop()
        mfSpeechState.value = SpeechState.IDLE
        mfIsASRActive.value = false
    }

    suspend fun switchModel(context: Context, model: AsrModels) {
        currentModelPreference.value = model
        if (model == AsrModels.AndroidFreeForm) {
            initAndroidSpeechRecognizer()
        } else {
            loadTfLiteModelFromGCS(context, model)
        }
    }

    /***********************************************************************************************
     * TF Lite
     **********************************************************************************************/
    private val mfTfLiteIsInferencing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val flowTfLiteIsInferencing: StateFlow<Boolean> = mfTfLiteIsInferencing
    private val mfTfLiteCurrentInferencingDuration: MutableStateFlow<Int> = MutableStateFlow(0)
    val flowTfLiteCurrentInferencingDuration: StateFlow<Int> = mfTfLiteCurrentInferencingDuration
    private val mfTfLiteLastRecordSize: MutableStateFlow<Int> = MutableStateFlow(0)
    val flowTfLiteLastRecordSize: StateFlow<Int> = mfTfLiteLastRecordSize
    private val mfTfLiteLastLogMelInferencingDuration: MutableStateFlow<Int> = MutableStateFlow(0)
    val flowTfLiteLastLogMelDuration: StateFlow<Int> =
        mfTfLiteLastLogMelInferencingDuration
    private val mfTfLiteLastEncoderDecoderInferencingDuration: MutableStateFlow<Int> =
        MutableStateFlow(0)
    val flowTfLiteLastEncoderDecoderDuration: StateFlow<Int> =
        mfTfLiteLastEncoderDecoderInferencingDuration
    private val mfTfLiteAccumulatorSize: MutableStateFlow<Int> = MutableStateFlow(0)
    val flowTfLiteAccumulatorSize: StateFlow<Int> = mfTfLiteAccumulatorSize

    private var encoderDecoder: EncoderDecoder? = null

    private var scopeTfLiteSpeechRecognizer: CoroutineScope? = null

    //= CoroutineScope(parentScope.coroutineContext)
    private var melSpectrogram: TFLiteLogMel? = null
    private var tokenizer: WhisperTokenizer? = null


    private fun freeTFLite() {
        melSpectrogram?.close()
        encoderDecoder?.close()
        melSpectrogram = null
        encoderDecoder = null
    }

    private suspend fun loadTfLiteModelFromGCS(context: Context, model: AsrModels) {
        mfIsReady.value = false
        if (melSpectrogram == null) {
            val logMelInterpreter = tfLiteFactory.createInterpreterFromGCS(
                context, gcsFeaturesExtractorPath(), "logmel"
            )
            melSpectrogram = TFLiteLogMel(logMelInterpreter)
        }

        if (tokenizer == null) tokenizer = WhisperTokenizer.loadFromGCS(context)

        withContext(Dispatchers.IO) {
            // handle blocking call
            encoderDecoder?.close()
        }
        encoderDecoder = null

        val decoderEncoderInterpreter = tfLiteFactory.createInterpreterFromGCS(
            context, gcsEncoderDecoderPath(model), "encoder-decoder"
        )
        encoderDecoder = EncoderDecoder(decoderEncoderInterpreter)
        mfIsReady.value =
            melSpectrogram != null && encoderDecoder != null && tokenizer != null
        Log.d(TAG, "Model $model ready")
    }

    private fun startTfLite(
        parentScope: CoroutineScope,
        classesFlow: Flow<IVoiceClassifier.Classes>,
    ) {
        mfSpeechState.value = SpeechState.NON_SPEECH
        val melSpectrogram = this.melSpectrogram ?: return
        val encoderDecoder = this.encoderDecoder ?: return
        val tokenizer = this.tokenizer ?: return

        mfIsASRActive.value = true
        var inferencingTimerJob: Job? = null

        scopeTfLiteSpeechRecognizer?.cancel()
        val scopeTfLiteSpeechRecognizer = CoroutineScope(parentScope.coroutineContext)

        classesFlow
            .accumulate16000(mfTfLiteAccumulatorSize)
            .filter {
                it.isNotEmpty()
            }
            .conflate()
            .onEach {
                mfTfLiteLastRecordSize.value = (it.size / 16000 + .5).toInt()
                mfTfLiteCurrentInferencingDuration.value = 0

                inferencingTimerJob?.cancel()
                inferencingTimerJob = scopeTfLiteSpeechRecognizer.launch(Dispatchers.IO) {
                    val ts = System.currentTimeMillis()
                    mfTfLiteCurrentInferencingDuration.value = 0
                    Log.d(TAG, "Inferencing timer started ts=$ts")
                    try {
                        while (true) {
                            delay(1000)
                            mfTfLiteCurrentInferencingDuration.value =
                                ((System.currentTimeMillis() - ts) / 1000).toInt()
                        }
                    } finally {
                        Log.d(TAG, "Inferencing timer stopped")
                    }
                }
            }
            .whisper(
                melSpectrogram,
                encoderDecoder,
                tokenizer,
                mfTfLiteIsInferencing,
                mfTfLiteLastLogMelInferencingDuration,
                mfTfLiteLastEncoderDecoderInferencingDuration,
            ).onEach {
                mfASR.emit(it)
                inferencingTimerJob?.cancel()
                inferencingTimerJob = null
            }.launchIn(scopeTfLiteSpeechRecognizer)
    }

    private fun stopTfLite() {
        scopeTfLiteSpeechRecognizer?.cancel()
        scopeTfLiteSpeechRecognizer = null
    }

    /*****************************************************************************
     * Android Speech Recognizer
     ****************************************************************************/

    private var jobAndroidSpeechRecognizer: Job? = null

    private suspend fun initAndroidSpeechRecognizer() {
        mfIsReady.value = false
        yield()
        mfIsReady.value = true
    }

    // TODO: coroutineScope has no error handler
    // TODO: handle permission
    @SuppressLint("MissingPermission")
    private fun startAndroidSpeechRecognizer(
        androidContext: Context,
        feedingScope: CoroutineScope,
        classesFlow: Flow<IVoiceClassifier.Classes>,
    ) {
        stopTfLite()
        stopAndroidSpeechRecognizer()
        mfIsASRActive.value = true
        jobAndroidSpeechRecognizer = classesFlow
            .androidSpeechRecognitions(
                androidContext,
                {
                    audioService.stopRecording()
                    Log.d(TAG, "turn off voice recording")
                },
                {
                    audioService.startRecording()
                    Log.d(TAG, "turn on voice recording")
                })
            .onEach {
                mfASR.emit(it)
            }
            .launchIn(feedingScope)
    }

    private fun stopAndroidSpeechRecognizer() {
        jobAndroidSpeechRecognizer?.cancel()
        jobAndroidSpeechRecognizer = null
        mfSpeechState.value = SpeechState.IDLE
        mfIsASRActive.value = false
    }
}