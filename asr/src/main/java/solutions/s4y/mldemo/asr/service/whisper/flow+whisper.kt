package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import solutions.s4y.audio.mel.IMelSpectrogram
import kotlin.coroutines.coroutineContext

fun Flow<FloatArray>.whisper(
    melSpectrogram: IMelSpectrogram,
    model: EncoderDecoder,
    tokenizer: WhisperTokenizer,
    mutableStateFlow: MutableStateFlow<Boolean>,
    skipSpecial: Boolean = true,
    compactSameSpecialTokens: Boolean = true
): Flow<String> = flow {
    collect { waveForms ->
        if (mutableStateFlow.value) return@collect
        mutableStateFlow.value = true
        try {
            if (!coroutineContext.isActive) return@collect
            val logMel = melSpectrogram.getMelSpectrogram(waveForms)
            if (!coroutineContext.isActive) return@collect
            val tokens = model.transcribe(logMel)
            if (!coroutineContext.isActive) return@collect
            val text = tokenizer.decode(tokens, skipSpecial, compactSameSpecialTokens)
            emit(text)
        } finally {
            mutableStateFlow.value = false
        }
    }
}