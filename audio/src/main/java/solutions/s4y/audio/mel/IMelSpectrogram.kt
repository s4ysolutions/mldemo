package solutions.s4y.audio.mel

interface IMelSpectrogram {
    suspend fun getMelSpectrogram(waveForms: FloatArray): FloatArray
}