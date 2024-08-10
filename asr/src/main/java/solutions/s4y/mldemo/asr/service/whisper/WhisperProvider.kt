package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.flow.map
import solutions.s4y.audio.accumulator.WaveFormsAccumulator
import solutions.s4y.audio.mel.IMelSpectrogram
import kotlin.math.min

class WhisperProvider(
    private val waveFormsAccumulator: WaveFormsAccumulator,
    private val melSpectrogram: IMelSpectrogram,
    private val model: WhisperInferrer,
    private val tokenizer: WhisperTokenizer
) {
    init {
        waveFormsAccumulator.batch = 16000 * 5
    }

    internal fun waveFormsToMel(waveForms: FloatArray): FloatArray {
        val inb = if (waveForms.size == 16000 * 30)
            waveForms
        else {
            val b = FloatArray(16000 * 30)
            waveForms.copyInto(b, endIndex = min(16000 * 30, waveForms.size))
            b
        }
        val mel = melSpectrogram.getMelSpectrogram(inb)
        return mel
    }

    internal fun waveFormsToTokens(waveForms: FloatArray): IntArray {
        val mel = waveFormsToMel(waveForms)
        val tokens = model.runInference(mel)
        return tokens
    }

    internal fun decodeWaveForms(waveForms: FloatArray): String {
        val tokens = waveFormsToTokens(waveForms)
        return tokenizer.decode(tokens)
    }

    val decodingFlow = waveFormsAccumulator.flow.map(::decodeWaveForms)
}