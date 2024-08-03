package solutions.s4y.audio.mel

interface IMelSpectrogramProvider {
    fun getMelSpectrogram(samples: FloatArray, nSamples: Int): FloatArray
}