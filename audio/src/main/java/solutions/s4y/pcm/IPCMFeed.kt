package solutions.s4y.pcm

import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface IPCMFeed: Closeable {
    var batch: Int
    val flow: Flow<FloatArray>
    fun addSamples(shortsArray: ShortArray)
    fun reset()
}