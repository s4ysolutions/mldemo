package solutions.s4y.audio.mel

interface IMelSpectrogram {
    fun getMelSpectrogram(samples: FloatArray): FloatArray
}