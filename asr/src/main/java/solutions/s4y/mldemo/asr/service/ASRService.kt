package solutions.s4y.mldemo.asr.service

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.mldemo.asr.service.gcs.gcsEncoderDecoderPath
import solutions.s4y.mldemo.asr.service.gcs.gcsFeaturesExtractorPath
import solutions.s4y.mldemo.asr.service.whisper.EncoderDecoder
import solutions.s4y.mldemo.asr.service.logmel.TFLiteLogMel
import solutions.s4y.mldemo.asr.service.whisper.SpeechState
import solutions.s4y.mldemo.asr.service.whisper.WhisperTokenizer
import solutions.s4y.mldemo.asr.service.whisper.accumulate16000
import solutions.s4y.mldemo.asr.service.whisper.whisper
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import solutions.s4y.tflite.base.TfLiteFactory
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton

class ASRService @Inject constructor(private val tfLiteFactory: TfLiteFactory) : Closeable {
    private lateinit var melSpectrogram: IMelSpectrogram
    private lateinit var encoderDecoder: EncoderDecoder
    private lateinit var tokenizer: WhisperTokenizer

    private val mutableFlow: MutableSharedFlow<String> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val mutableFlowSpeechState: MutableStateFlow<SpeechState> =
        MutableStateFlow(SpeechState.IDLE)
    private val mutableFlowModelReady: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    private val mutableFlowIsDecoding: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    private var jobWhisper: Job? = null

    val flow: SharedFlow<String> = mutableFlow
    val flowVoiceState: StateFlow<SpeechState> = mutableFlowSpeechState
    val flowModelReady: StateFlow<Boolean> = mutableFlowModelReady
    val flowIsDecoding: StateFlow<Boolean> = mutableFlowIsDecoding

    override fun close() {
        jobWhisper?.cancel()
        mutableFlowSpeechState.value = SpeechState.IDLE
    }

    suspend fun loadModelFromGCS(context: Context, model: EncoderDecoder.Models) {
        stop()
        mutableFlowModelReady.value = false

        val logMelInterpreter = tfLiteFactory.createInterpreterFromGCS(
            context,
            gcsFeaturesExtractorPath(),
            "logmel"
        )
        melSpectrogram = TFLiteLogMel(logMelInterpreter)

        val decoderEncoderInterpreter = tfLiteFactory.createInterpreterFromGCS(
            context,
            gcsEncoderDecoderPath(model),
            "encoder-decoder"
        )
        encoderDecoder = EncoderDecoder(decoderEncoderInterpreter)

        tokenizer = WhisperTokenizer.loadFromGCS(context)

        mutableFlowModelReady.value = true
    }

    fun start(classesFlow: Flow<IVoiceClassifier.Classes>, feedingScope: CoroutineScope) {
        mutableFlowSpeechState.value = SpeechState.NON_SPEECH
        jobWhisper = classesFlow
            .conflate()
            .accumulate16000(mutableFlowSpeechState)
            .filter { it.isNotEmpty() }
            .whisper(melSpectrogram, encoderDecoder, tokenizer, mutableFlowIsDecoding)
            .onEach {
                mutableFlow.emit(it)
            }
            .launchIn(feedingScope)
    }

    fun stop() {
        jobWhisper?.cancel()
        jobWhisper = null
        mutableFlowSpeechState.value = SpeechState.IDLE
    }
}