package solutions.s4y.pcm

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class PCMFeed : IPCMFeed {
    companion object {
        fun absMax(arr: ShortArray): Int {
            var max = 0
            for (s in arr) {
                // no good abs for short
                if (s >= 0) {
                    if (s > max) {
                        max = s.toInt()
                    }
                } else {
                    if (-s > max) {
                        max = -s
                    }
                }
            }
            return max
        }
    }
    private val SAMPLE_RATE = 16000

    private var _batch = SAMPLE_RATE
    private val _waveForms = MutableSharedFlow<FloatArray>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val samplesArrays = mutableListOf<ShortArray>()
    private var samplesCount = 0

    private val sync = Any()

    override var batch: Int
        get() = _batch
        set(value) {
            _batch = value
            reset()
        }

    override fun addSamples(shortArray: ShortArray): Unit = synchronized(sync) {
        samplesCount += shortArray.size
        val above = samplesCount - _batch
        if (above >= 0) {
            val samplesLast = if (above > 0)
                shortArray.copyOfRange(0, shortArray.size - above)
            else
                shortArray
            val maxL = absMax(samplesLast)
            var max = 0
            var total = 0
            for (arr in samplesArrays) {
                val max0 = absMax(arr)
                if (max0 > max) {
                    max = max0
                }
                total += arr.size
            }
            max = if (maxL > max) maxL else max
            val floatArray = FloatArray(total + samplesLast.size)
            if (max == 0) {
                floatArray.fill(0.0f)
            } else {
                var i = 0
                for (arr in samplesArrays) {
                    for (s in arr) {
                        floatArray[i++] = s.toFloat() / max
                    }
                }
                for (s in samplesLast) {
                    floatArray[i++] = s.toFloat() / max
                }
            }
            _waveForms.tryEmit(floatArray)
            reset()
        } else {
            samplesArrays.add(shortArray)
        }
    }

    override fun reset() = synchronized(sync) {
        samplesArrays.clear()
        samplesCount = 0
    }

    override val waveForms: SharedFlow<FloatArray> = _waveForms
}