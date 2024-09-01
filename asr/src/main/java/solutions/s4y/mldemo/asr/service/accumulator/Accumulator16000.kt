package solutions.s4y.mldemo.asr.service.accumulator

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.max

// TODO: this should be an operator
class Accumulator16000 {
    private var sequenceBuffer: FloatArray = FloatArray(0)
    private val lock: Mutex = Mutex()

    fun reset() = runBlocking {
        lock.withLock {
            if (sequenceBuffer.isNotEmpty())
                sequenceBuffer = FloatArray(0)
        }
    }

    suspend fun add(waveForms: FloatArray) = lock.withLock {
        if (sequenceBuffer.size + waveForms.size > MAX_CAPACITY) {
            val srcOffset = max(0, waveForms.size - MAX_CAPACITY)
            // sift by lenth of waveForms
            sequenceBuffer.copyInto(
                sequenceBuffer,
                0,
                waveForms.size - srcOffset,
                sequenceBuffer.size
            )
            sequenceBuffer
        } else {
            val newBuffer = FloatArray(sequenceBuffer.size + waveForms.size)
            sequenceBuffer.copyInto(newBuffer)
            waveForms.copyInto(newBuffer, sequenceBuffer.size)
            sequenceBuffer = newBuffer
        }
    }

    suspend fun duration(): Int = lock.withLock {
        (sequenceBuffer.size / FREQ + .5).toInt()
    }

    suspend fun size(): Int = lock.withLock { sequenceBuffer.size }

    suspend fun isEmpty(): Boolean = lock.withLock { sequenceBuffer.isEmpty() }

    suspend fun waveForms(): FloatArray = lock.withLock { sequenceBuffer }

    companion object {
        const val FREQ = 16000
        const val MAX_CAPACITY = FREQ * 30
    }
}

