package solutions.s4y.mldemo.voice_transcription.service.dependencies

import kotlinx.coroutines.flow.SharedFlow

interface IWaveFormsAccumulator {
    val waveFormsFlow: SharedFlow<FloatArray>
    var threshold: Int
    fun addWaveForm(waveForm: FloatArray)
    fun reset()
}