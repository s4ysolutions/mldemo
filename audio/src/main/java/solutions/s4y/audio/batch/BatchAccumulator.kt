package solutions.s4y.audio.batch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.LinkedList


internal class BatchAccumulator(private val batchSize: Int = SAMPLE_RATE * 5) {
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
        if (batches.size > 100) {
            println("acc is full, dropping oldest batch")
            batches.removeFirst()
        }
        val freeSpace = accumulator.size - accumulatorPos
        println("add ${waveForms.size} samples to the buffer with $freeSpace free space")
        if (freeSpace >= waveForms.size) {
            System.arraycopy(waveForms, 0, accumulator, accumulatorPos, waveForms.size)
            if (freeSpace == waveForms.size) {
                batches.add(accumulator)
                println("acc has grown to ${batches.size} batches")
                accumulator = FloatArray(batchSize)
                accumulatorPos = 0
            } else
                accumulatorPos += waveForms.size
            println("accumulating next from $accumulatorPos")
        } else {
            System.arraycopy(waveForms, 0, accumulator, accumulatorPos, freeSpace)
            batches.add(accumulator)
            println("acc has grown to ${batches.size} batches")
            accumulator = FloatArray(batchSize)
            accumulatorPos = 0
            println("accumulating next from $accumulatorPos")
            addLocked(waveForms.copyOfRange(freeSpace, waveForms.size))
        }
    }

    suspend fun add(waveForms: FloatArray) {
        if (waveForms.isNotEmpty()) {
            accumulatorMutex.withLock {
                addLocked(waveForms)
            }
        }
    }

    suspend fun add(pcm: ShortArray) =
        add(FloatArray(pcm.size) { i -> pcm[i].toFloat() / Short.MAX_VALUE })

    suspend fun batch(): FloatArray? = batchesMutex.withLock {
        println("batch request")
        if (batches.isNotEmpty()) {
            println("acc can emit up to ${batches.size} batches")
            batches.removeFirst()
        } else {
            println("acc is empty")
            null
        }
    }
}