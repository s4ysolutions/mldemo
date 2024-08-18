package solutions.s4y.audio.accumulator

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList


internal class MemoryWaveFormsAccumulator<T : Any, A : Any>(override val batch: Int = SAMPLE_RATE * 5) :
    IWaveFormsAccumulator<T, A> {

    companion object {
        private const val SAMPLE_RATE = 16000

        private fun List<FloatArray>.toFloatArray(): FloatArray {
            val result = FloatArray(sumOf { it.size })
            var offset = 0
            for (array in this) {
                System.arraycopy(array, 0, result, offset, array.size)
                offset += array.size
            }
            return result
        }
    }

    private val accumulatedArrays = LinkedList<FloatArray>() // synced
    private var accumulatedSize: Int = 0 // synced
    private val accumulatedMutex: Mutex = Mutex()


    // private val lock = Any()
    private val mutableSharedFlow: MutableSharedFlow<FloatArray> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.SUSPEND)


    // private fun addFloatWaveForms(waveForms: FloatArray) = addFloatWaveFormsSync(waveForms)

    private suspend fun addFloatWaveForms(waveForms: FloatArray) {
        if (waveForms.isNotEmpty()) {
            accumulatedMutex.withLock {
                accumulatedArrays.add(waveForms)
                accumulatedSize += waveForms.size
            }
        }
    }

    /*
        private fun backpressure(flushArrays: List<FloatArray>) =
            synchronized(lock) {
                accumulatedArrays.addAll(0, flushArrays)
                accumulatedSize += flushArrays.sumOf { it.size }
            }
    */
    private suspend fun backpressure(flushArray: FloatArray) = accumulatedMutex.withLock {
        accumulatedArrays.add(0, flushArray)
        accumulatedSize += flushArray.size
    }

    // private var _batch: Int = batch // length of output batch
    private val haveToFlush: Boolean get() = accumulatedSize >= batch

    private suspend fun getFlushArray(flushSize: Int): FloatArray =
        accumulatedMutex.withLock {
            if (flushSize > accumulatedSize)
                return FloatArray(0)
            if (accumulatedArrays[0].size == flushSize) {
                val flushArray = accumulatedArrays.removeAt(0)
                accumulatedSize -= flushSize
                return flushArray
            }
            val flushArrays = LinkedList<FloatArray>()
            var flushedSize = 0
            while (flushedSize < flushSize) {
                val toFlushSize = flushSize - flushedSize
                val array = accumulatedArrays[0]
                if (array.size <= toFlushSize) {
                    flushedSize += array.size
                    flushArrays.add(accumulatedArrays.removeAt(0))
                } else {
                    val newArray = array.copyOfRange(0, toFlushSize)
                    val rest = array.copyOfRange(toFlushSize, array.size)
                    accumulatedArrays[0] = rest
                    flushedSize += newArray.size
                    flushArrays.add(newArray)
                }
                // this condition is always false due to  flushSize <= accumulatedSize
                // if (accumulatedArrays.isEmpty()) break
            }

            /* this condition is always false due to  flushSize <= accumulatedSize
            if (flushedSize < flushSize) {
                // backpressure
                flushArrays.addAll(0, accumulatedArrays)
                return Pair(emptyList(), 0)
            }
             */

            accumulatedSize -= flushedSize

            // TODO: remove some day
            assert(flushedSize == flushSize)
            assert(accumulatedSize >= 0)
            return flushArrays.toFloatArray()
        }

    private suspend fun tryEmit(flushArray: FloatArray): Boolean {
        // TODO: remove some day
        assert(flushArray.isNotEmpty())
        println("=====> mutableSharedFlow.subscrcount ${mutableSharedFlow.subscriptionCount.value}")
        if (mutableSharedFlow.tryEmit(flushArray)) return true
        backpressure(flushArray)
        return false
    }

    private suspend fun emit(flushArray: FloatArray) = mutableSharedFlow.emit(flushArray)

    /*
    override var batch: Int
        get() = synchronized(lock) { _batch }
        set(value) {
            if (value <= 0) {
                reset()
                synchronized(lock) {
                    _batch = value
                }
            } else
                synchronized(lock) {
                    if (batch >= _batch) {
                        _batch = value
                    } else {
                        while (accumulatedArrays.size > 0) {
                            // do not try remove exact size for performance reasons
                            val deleted = accumulatedArrays.removeAt(0)
                            accumulatedSize -= deleted.size
                            // TODO: remove some day
                            assert(accumulatedSize >= 0)
                            if (accumulatedSize <= batch) {
                                break
                            }
                        }
                    }
                }
        }*/

    override suspend fun add(waveForms: FloatArray) {
        addFloatWaveForms(waveForms)
        while (haveToFlush)
            emit(getFlushArray(batch))
    }

    override suspend fun tryAdd(waveForms: FloatArray): Boolean {
        addFloatWaveForms(waveForms)
        while (haveToFlush) {
            val flushArray = getFlushArray(batch)
            if (!tryEmit(flushArray)) break
        }
        return !haveToFlush
    }


    override fun close() {
    }

    override val flow = mutableSharedFlow

    override suspend fun flush(): Boolean {
        while (haveToFlush) {
            val flushArray = getFlushArray(batch)
            if (!tryEmit(flushArray)) break
        }
        return haveToFlush
    }

    override suspend fun reset(): Unit = accumulatedMutex.withLock {
        accumulatedArrays.clear()
        accumulatedSize = 0
    }
}