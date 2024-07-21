package solutions.s4y.mel

interface IMelSpectrogramProvider {
    fun getMelSpectrogram(samples: FloatArray, nSamples: Int): FloatArray
}