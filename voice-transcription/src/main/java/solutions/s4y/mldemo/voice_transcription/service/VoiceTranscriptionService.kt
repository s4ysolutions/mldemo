package solutions.s4y.mldemo.voice_transcription.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import solutions.s4y.mldemo.voice_transcription.service.dependencies.IMelVoiceTranscriber
import solutions.s4y.mldemo.voice_transcription.service.dependencies.IWaveFormsAccumulator
import solutions.s4y.mldemo.voice_transcription.service.implementation.KotlinMelSpectrogramTransformer

class VoiceTranscriptionService(
    private val accumulator: IWaveFormsAccumulator,
    private val melSpectrogramProvider: KotlinMelSpectrogramTransformer,
    private val transcriber: IMelVoiceTranscriber
) {
    val transcriptionsFlow: Flow<List<String>> = accumulator.waveFormsFlow
        .map{melSpectrogramProvider.getMelSpectrogram(it)}
        .map { transcriber.transcribe(it) }

    // Array of -1..1 values ~ 1 sec
    fun addWaveForm(waveForm: FloatArray) = accumulator.addWaveForm(waveForm)
}