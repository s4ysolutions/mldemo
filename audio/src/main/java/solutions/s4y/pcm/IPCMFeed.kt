package solutions.s4y.pcm

import kotlinx.coroutines.flow.Flow

interface IPCMFeed {
    var batch: Int
    val waveForms: Flow<FloatArray>
    fun addSamples(shortArray: ShortArray)
    fun reset()
}