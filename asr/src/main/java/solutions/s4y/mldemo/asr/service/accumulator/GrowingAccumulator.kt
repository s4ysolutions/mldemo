package solutions.s4y.mldemo.asr.service.accumulator

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// TODO: this should be an operator
class GrowingAccumulator() {
    private var sequenceBuffer: FloatArray = FloatArray(0)
    private val lock: Mutex = Mutex()

    suspend fun reset() = lock.withLock {
            println("reset")
            sequenceBuffer = FloatArray(0)
    }

    suspend fun growAccumulator(waveForms: FloatArray) = lock.withLock {
        val newBuffer = FloatArray(sequenceBuffer.size + waveForms.size)
        sequenceBuffer.copyInto(newBuffer)
        waveForms.copyInto(newBuffer, sequenceBuffer.size)
        sequenceBuffer = newBuffer
        newBuffer
    }
}

