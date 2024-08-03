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

    private val accumulatedArrays = LinkedList<FloatArray>()
    private var accumulatedSize: Int = 0
    private val lock = Any()
    private var onAddWaveForm: (FloatArray) -> Unit = {}
    private var onClose: () -> Unit = {}

    // length of waveforms in number of samples to be flushed
    private var _batch: Int = batch

    private fun addFloatWaveForms(waveForms: FloatArray) {
        if (waveForms.isEmpty()) {
            return
        }
        val (flushArrays, flushSize) = synchronized(lock) {
            addWaveFormsWithFlushNonSync(waveForms)
        }
        if (!flushArrays.isNullOrEmpty()) {
            val dst = FloatArray(flushSize)
            var offset = 0
            for (src in flushArrays) {
                System.arraycopy(src, 0, dst, offset, src.size)
                offset += src.size
            }
            onAddWaveForm(dst)
        }
    }

    private fun addShortWaveForms(waveForms: ShortArray) {
        if (waveForms.isEmpty()) {
            return
        }
        val floatWaveForms =
            FloatArray(waveForms.size) { i ->
                waveForms[i].toFloat() / Short.MAX_VALUE
            }
        addFloatWaveForms(floatWaveForms)
    }

    private fun addWaveFormsNonSync(waveForms: FloatArray) {
        if (waveForms.isEmpty()) {
            return
        }
        val above = accumulatedSize + waveForms.size - _batch
        if (above < 0) {
            accumulatedArrays.add(waveForms)
            accumulatedSize += waveForms.size
        } else {
            var rest: FloatArray? = null
            if (above == 0) {
                accumulatedArrays.add(waveForms)
                accumulatedSize += waveForms.size
            } else { // above > 0
                val size = if (waveForms.size < _batch) waveForms.size else _batch
                val array = waveForms.copyOfRange(0, size)
                rest = waveForms.copyOfRange(size, waveForms.size)
                accumulatedArrays.add(array)
                accumulatedSize += array.size
            }
            if (rest != null) {
                addWaveFormsNonSync(rest)
            }
        }
    }

    private fun addWaveFormsWithFlushNonSync(waveForms: FloatArray): Pair<List<FloatArray>?, Int> {
        addWaveFormsNonSync(waveForms)
        if (accumulatedSize < _batch) {
            return Pair(null, 0)
        } else {
            val flushSize = _batch
            return Pair(getFlushListNonSync(flushSize), flushSize)
        }
    }

    private fun resetNonSync() {
        accumulatedArrays.clear()
        accumulatedSize = 0
    }

    /**
     * Get the list of waveforms to be flushed.
     * so flushed size > batch is not possible
     * @param flushSize the size of the waveforms to be flushed
     * @return the list of waveforms to be flushed
     */
    private fun getFlushListNonSync(flushSize: Int): List<FloatArray> {
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
        accumulatedSize -= flushedSize

        // TODO: remove some day
        assert(flushedSize == flushSize || (flushedSize < flushSize && accumulatedArrays.isEmpty()))
        assert(flushedSize == flushSize)
        assert(accumulatedSize >= 0)
        return flushArrays
    }

    override val flow: Flow<FloatArray> = callbackFlow {
        onAddWaveForm = { trySend(it) }
        onClose = { close() }
        awaitClose()
    }

    override var batch: Int
        get() = synchronized(lock) { _batch }
        set(value) {
            if (value <= 0) {
                reset()
            } else
                synchronized(lock) {
                    if (batch >= _batch) {
                        _batch = value
                    } else {
                        while (accumulatedArrays.size > 0) {
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

    override fun add(waveForms: A) {
        when (waveForms) {
            is ShortArray -> addShortWaveForms(waveForms)
            is FloatArray -> addFloatWaveForms(waveForms)
            else -> throw IllegalArgumentException("Unsupported waveforms type: ${waveForms::class}")
        }
    }

    override fun close() {
        reset()
        onClose()
    }

    override fun reset(): Unit =
        synchronized(lock) {
            resetNonSync()
        }
}