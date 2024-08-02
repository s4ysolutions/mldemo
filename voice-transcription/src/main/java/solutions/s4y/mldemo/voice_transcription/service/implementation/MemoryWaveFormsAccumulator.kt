package solutions.s4y.mldemo.voice_transcription.service.implementation

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.pytorch.BuildConfig
import solutions.s4y.mldemo.voice_transcription.service.dependencies.IWaveFormsAccumulator

class MemoryWaveFormsAccumulator(threshold: Int = 16000 * 5): IWaveFormsAccumulator {
    private val _next = MutableSharedFlow<FloatArray>(
        replay = 1,
        extraBufferCapacity = 3,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _buffer = mutableListOf<FloatArray>()

    // length of accumulated waveforms in number of samples
    private var _size: Int = 0

    // length of waveforms in number of samples to be flushed
    private var _threshold: Int = threshold
    private val lock = Any()

    override val waveFormsFlow: SharedFlow<FloatArray> = _next
    override var threshold: Int
        get() = synchronized(lock) { _threshold }
        set(value) {
            if (value <= 0) {
                reset()
            } else
                synchronized(lock) {
                    if (threshold >= _threshold) {
                        _threshold = value
                    } else {
                        while (_buffer.size > 0) {
                            val deleted = _buffer.removeAt(0).size
                            _size -= deleted
                            if (BuildConfig.DEBUG) {
                                assert(_size >= 0)
                            }
                            if (_size <= threshold) {
                                break
                            }
                        }
                    }
                }
        }

    // called in the synchronized context
    // assume the last added waveform was already
    // aligned according to the threshold
    // so flashed size > threshold is not possible
    private fun getFlushListSync(flushSize: Int): List<FloatArray> {
        val flushArrays = mutableListOf<FloatArray>()
        var flushedSize = 0
        while (flushedSize + _buffer[0].size < flushSize) {
            val array = _buffer.removeAt(0)
            flushedSize += array.size
            flushArrays.add(array)
            if (_buffer.isEmpty()) {
                break
            }
        }
        _size -= flushedSize
        if (BuildConfig.DEBUG) {
            assert(flushedSize == flushSize)
            assert(_size >= 0)
        }
        return flushArrays
    }


    override fun addWaveForm(waveForm: FloatArray) {
        val (flushArrays, flushSize) = synchronized(lock) {
            val above = _size + waveForm.size - _threshold
            if (above < 0) {
                _buffer.add(waveForm)
                _size += waveForm.size
                Pair(null, 0)
            } else {
                if (above == 0) {
                    _buffer.add(waveForm)
                    _size += waveForm.size
                } else { // above > 0
                    val array = waveForm.copyOfRange(0, above)
                    val rest = waveForm.copyOfRange(above, waveForm.size)
                    _buffer.add(array)
                    _buffer.add(rest)
                    _size += waveForm.size
                }
                val flushSize = _threshold
                Pair(getFlushListSync(flushSize), flushSize)
            }
        }
        if (flushArrays != null) {
            val array = FloatArray(flushSize)
            var offset = 0
            for (src in flushArrays) {
                System.arraycopy(src, 0, array, offset, src.size)
                offset += src.size
            }
            _next.tryEmit(array)
        }
    }

    override fun reset() {
        synchronized(lock) {
            _buffer.clear()
            _size = 0
        }
    }
}