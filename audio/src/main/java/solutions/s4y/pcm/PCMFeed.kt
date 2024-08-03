package solutions.s4y.pcm

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PCMFeed: IPCMFeed {
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

        private const val SAMPLE_RATE = 16000
    }

    private var onAddPCM: (FloatArray) -> Unit = {}
    private var onClosePCM: () -> Unit = {}

    override fun close() {
        onClosePCM()
    }

    private var _batch = SAMPLE_RATE
    private val samplesArrays = mutableListOf<ShortArray>()
    private var samplesCount = 0

    private val sync = Any()

    override var batch: Int
        get() = _batch
        set(value) {
            // TODO: synchronization?
            _batch = value
            reset()
        }

    private fun addSamplesNotSync(shortsArray: ShortArray) {
        samplesCount += shortsArray.size
        val above = samplesCount - _batch
        if (above >= 0) {
            // array is to fulfill the batch
            // and rest is the left for the next batch
            val array: ShortArray
            val rest: ShortArray?
            if (above > 0) {
                array = shortsArray.copyOfRange(0, shortsArray.size - above)
                rest = shortsArray.copyOfRange(shortsArray.size - above, shortsArray.size)
            } else {
                array = shortsArray
                rest = null
            }
            // TODO: optimization hint
            // probably it is simpler to use Short.MaxValue
            val maxL = absMax(array)
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
            // normalize to -1.0..1.0
            val floatArray = FloatArray(total + array.size)
            if (max == 0) {
                floatArray.fill(0.0f)
            } else {
                var i = 0
                for (arr in samplesArrays) {
                    for (s in arr) {
                        floatArray[i++] = s.toFloat() / max
                    }
                }
                for (s in array) {
                    floatArray[i++] = s.toFloat() / max
                }
            }
            // notify the flow
            onAddPCM(floatArray)
            resetNotSync()
            if (rest != null) {
                addSamplesNotSync(rest)
            }
        } else {
            samplesArrays.add(shortsArray)
        }
    }

    private fun resetNotSync() {
        samplesArrays.clear()
        samplesCount = 0
    }

    override fun addSamples(shortsArray: ShortArray): Unit = synchronized(sync) {
        addSamplesNotSync(shortsArray)
    }

    override fun reset() = synchronized(sync) {
        resetNotSync()
    }

    override val flow: Flow<FloatArray> = callbackFlow {
        onAddPCM = { trySend(it) }
        onClosePCM = { close() }
        awaitClose()
    }
}