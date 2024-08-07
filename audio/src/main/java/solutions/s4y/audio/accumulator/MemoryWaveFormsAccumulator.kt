package solutions.s4y.audio.accumulator

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.LinkedList


internal class MemoryWaveFormsAccumulator<T : Any, A : Any>(batch: Int = SAMPLE_RATE * 5) :
    IWaveFormsAccumulator<T, A> {

    companion object {
        private const val SAMPLE_RATE = 16000
    }

    private val accumulatedArrays = LinkedList<FloatArray>() // synced
    private var accumulatedSize: Int = 0 // synced
    private var _batch: Int = batch // length of output batch
    private val lock = Any()
    private var onAddWaveForm: (FloatArray) -> Unit = {}
    private var onClose: () -> Unit = {}


    private fun addFloatWaveForms(waveForms: FloatArray) = addFloatWaveFormsSync(waveForms)

    private fun addFloatWaveFormsSync(waveForms: FloatArray) = synchronized(lock) {
        if (waveForms.isNotEmpty()) {
            accumulatedArrays.add(waveForms)
            accumulatedSize += waveForms.size
        }
    }

    private fun addShortWaveForms(waveForms: ShortArray) {
        if (waveForms.isNotEmpty()) {
            val floatWaveForms =
                FloatArray(waveForms.size) { i ->
                    waveForms[i].toFloat() / Short.MAX_VALUE
                }
            addFloatWaveFormsSync(floatWaveForms)
        }
    }

    private fun getFlushArraySync(flushSize: Int): Pair<List<FloatArray>, Int> =
        synchronized(lock) {
            val flushArrays = LinkedList<FloatArray>()
            var flushedSize = 0
            while (flushedSize < flushSize) {
                val above = flushSize - flushedSize
                val array = accumulatedArrays[0]
                if (array.size <= above) {
                    flushedSize += array.size
                    flushArrays.add(accumulatedArrays.removeAt(0))
                } else {
                    val newArray = array.copyOfRange(0, above)
                    val rest = array.copyOfRange(above, array.size)
                    accumulatedArrays[0] = rest
                    flushedSize += newArray.size
                    flushArrays.add(newArray)
                }
                if (accumulatedArrays.isEmpty()) {
                    break
                }
            }

            if (flushedSize < flushSize) {
                return Pair(emptyList(), 0)
            }

            accumulatedSize -= flushedSize

            // TODO: remove some day
            assert(flushedSize == flushSize)
            assert(accumulatedSize >= 0)
            return Pair(flushArrays, flushSize)
        }

    private fun flushBatch() {
        val (flushArrays, flushSize) = getFlushArraySync(batch)
        if (flushArrays.isNotEmpty()) {
            val dst = FloatArray(flushSize)
            var offset = 0
            for (src in flushArrays) {
                System.arraycopy(src, 0, dst, offset, src.size)
                offset += src.size
            }
            onAddWaveForm(dst)
        }
    }

    private val haveToFlush: Boolean
        get() = synchronized(lock) { accumulatedSize >= batch }

    override val flow: Flow<FloatArray> = callbackFlow {
        onAddWaveForm = {
            val t = trySend(it)
            println(t)
        }
        onClose = {
            close()
        }
        awaitClose()
    }

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
        }

    override fun add(pcm: ShortArray) {
            addShortWaveForms(pcm)
        while (haveToFlush)
            flushBatch()
    }

    override fun add(waveForms: FloatArray) {
        addFloatWaveForms(waveForms)
        while (haveToFlush)
            flushBatch()
    }

    override fun close() {
        reset()
        onClose()
    }

    override fun reset(): Unit = synchronized(lock) {
        accumulatedArrays.clear()
        accumulatedSize = 0
    }
}