package solutions.s4y.audio.mel

import kotlinx.coroutines.Deferred

interface IMelSpectrogram {
    suspend fun getMelSpectrogram(waveForms: FloatArray): FloatArray?
}