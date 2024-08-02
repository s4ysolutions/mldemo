package solutions.s4y.mldemo.voice_transcription.service.dependencies

interface IMelSpectrogramTransformer {
    fun getMelSpectrogram(samples: FloatArray): FloatArray
}