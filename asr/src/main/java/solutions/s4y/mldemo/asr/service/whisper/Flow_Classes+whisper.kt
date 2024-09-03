package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import solutions.s4y.mldemo.asr.service.logmel.TFLiteLogMel
import kotlin.coroutines.coroutineContext

fun Flow<FloatArray>.whisper(
    melSpectrogram: TFLiteLogMel,
    model: EncoderDecoder,
    tokenizer: WhisperTokenizer,
    mutableIsInferencingFlow: MutableStateFlow<Boolean>,
    logMelInferencingDurationFlow: MutableStateFlow<Int>,
    encoderDecoderInferencingDurationFlow: MutableStateFlow<Int>,
    skipSpecial: Boolean = true,
    compactSameSpecialTokens: Boolean = true
): Flow<String> = flow {
    collect { waveForms ->
        if (mutableIsInferencingFlow.value) return@collect
        mutableIsInferencingFlow.value = true
        yield() // make value to be propogated
        try {
            if (!coroutineContext.isActive) return@collect
            val logMel = melSpectrogram.getMelSpectrogram(waveForms)
            logMelInferencingDurationFlow.value = melSpectrogram.duration
            yield() // make value to be propogated
            if (!coroutineContext.isActive) return@collect
            val tokens = model.transcribe(logMel)
            encoderDecoderInferencingDurationFlow.value = model.duration
            yield() // make value to be propogated
            if (!coroutineContext.isActive) return@collect
            val text = tokenizer.decode(tokens, skipSpecial, compactSameSpecialTokens)
            emit(text)
        } finally {
            mutableIsInferencingFlow.value = false
            yield()
        }
    }
}