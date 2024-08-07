package solutions.s4y.mldemo.asr.service

import kotlinx.coroutines.flow.map
import solutions.s4y.audio.accumulator.WaveFormsAccumulator
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.mldemo.asr.service.whisper.Whisper
import solutions.s4y.mldemo.asr.service.whisper.WhisperTokenizer
import kotlin.math.min

class ASRService(
    private val waveFormsAccumulator: WaveFormsAccumulator,
    private val melSpectrogram: IMelSpectrogram,
    private val model: Whisper,
    private val tokenizer: WhisperTokenizer
) {
    init {
        waveFormsAccumulator.batch = 16000 * 5
    }

    internal fun decodeWaveForms(waveForms: FloatArray): String {
        val inb = if (waveForms.size == 16000 * 30)
            waveForms
        else {
            val b = FloatArray(16000 * 30)
            waveForms.copyInto(b, endIndex = min(16000 * 30, waveForms.size))
            b
        }
        val mel = melSpectrogram.getMelSpectrogram(inb)
        val tokens = model.runInference(mel)
        return tokenizer.decode(tokens)
    }

    val decodingFlow = waveFormsAccumulator.flow.map(::decodeWaveForms)
}