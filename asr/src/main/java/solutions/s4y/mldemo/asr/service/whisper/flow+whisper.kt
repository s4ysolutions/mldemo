package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.mldemo.asr.service.logmel.TFLiteLogMel
import kotlin.coroutines.coroutineContext

fun Flow<FloatArray>.whisper(
    melSpectrogram: TFLiteLogMel,
    model: EncoderDecoder,
    tokenizer: WhisperTokenizer,
    mutableStateFlow: MutableStateFlow<Boolean>,
    logMelStateFlow: MutableStateFlow<Long>,
    encoderDecoderStateFlow: MutableStateFlow<Long>,
    skipSpecial: Boolean = true,
    compactSameSpecialTokens: Boolean = true
): Flow<String> = flow {
    collect { waveForms ->
        if (mutableStateFlow.value) return@collect
        mutableStateFlow.value = true
        yield() // make value to be propogated
        try {
            if (!coroutineContext.isActive) return@collect
            val logMel = melSpectrogram.getMelSpectrogram(waveForms)
            logMelStateFlow.value = melSpectrogram.duration
            yield() // make value to be propogated
            if (!coroutineContext.isActive) return@collect
            val tokens = model.transcribe(logMel)
            encoderDecoderStateFlow.value = model.duration
            yield() // make value to be propogated
            if (!coroutineContext.isActive) return@collect
            val text = tokenizer.decode(tokens, skipSpecial, compactSameSpecialTokens)
            emit(text)
        } finally {
            mutableStateFlow.value = false
            yield()
        }
    }
}