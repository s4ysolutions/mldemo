package solutions.s4y.audio.batch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList


internal class BatchAccumulator(
    private val batchSize: Int = SAMPLE_RATE * 5,
    private val capacity: Int = batchSize * 2
) {
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

    private val batches = LinkedList<FloatArray>() // synced
    private val batchesMutex: Mutex = Mutex()

    private var accumulator = FloatArray(batchSize)
    private var accumulatorPos = 0
    private val accumulatorMutex = Mutex()


    private fun addLocked(waveForms: FloatArray) {
        while (batches.size + waveForms.size > capacity) {
            println("acc is full, dropping oldest batch")
            batches.removeFirst()
        }
        val freeSpace = accumulator.size - accumulatorPos
        if (freeSpace >= waveForms.size) {
            System.arraycopy(waveForms, 0, accumulator, accumulatorPos, waveForms.size)
            if (freeSpace == waveForms.size) {
                batches.add(accumulator)
                accumulator = FloatArray(batchSize)
                accumulatorPos = 0
            } else
                accumulatorPos += waveForms.size
        } else {
            System.arraycopy(waveForms, 0, accumulator, accumulatorPos, freeSpace)
            batches.add(accumulator)
            accumulator = FloatArray(batchSize)
            accumulatorPos = 0
            addLocked(waveForms.copyOfRange(freeSpace, waveForms.size))
        }
    }

    suspend fun add(waveForms: FloatArray) {
        if (waveForms.isNotEmpty()) {
            if (waveForms.size > capacity) {
                accumulatorMutex.withLock {
                    addLocked(waveForms.copyOfRange(waveForms.size - capacity, waveForms.size))
                }
            } else {
                accumulatorMutex.withLock {
                    addLocked(waveForms)
                }
            }
        }
    }

    suspend fun add(pcm: ShortArray) =
        add(FloatArray(pcm.size) { i -> pcm[i].toFloat() / Short.MAX_VALUE })

    suspend fun batch(): FloatArray? = batchesMutex.withLock {
        if (batches.isNotEmpty()) {
            batches.removeFirst()
        } else {
            null
        }
    }

    suspend fun content(): FloatArray = batchesMutex.withLock {
        val result = FloatArray(batchSize * batches.size + accumulatorPos) { index ->
            val batchIndex = index / batchSize
            val firstIndex = batchIndex * batchSize
            if (batchIndex < batches.size) {
                batches[batchIndex][index - firstIndex]
            } else {
                accumulator[index - firstIndex]
            }
        }
        result
    }
}