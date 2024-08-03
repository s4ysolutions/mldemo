package solutions.s4y.pcm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import solutions.s4y.audio.accumulator.WaveFormsAccumulator


class WaveFormsAccumulatorTest {
    @Test
    fun flow_shouldNotEmit_WhenBelowBatch() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(10)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        val job = launch {
            accumulator.flow
                .onEach(cb)
                .toList(results)
        }
        delay(1)
        accumulator.add(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f))
        accumulator.close()
        job.join()

        // Assert
        assertTrue(results.isEmpty())
        verify(cb, never()).invoke(any())
    }

    @Test
    fun flow_shouldEmit_WhenBatch() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(10)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        val job = launch {
            accumulator.flow
                .onEach(cb)
                .toList(results)
        }
        delay(1)
        accumulator.add(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f))
        accumulator.close()
        job.join()

        // Assert
        assertEquals(1, results.size)
        assertEquals(10, results[0].size)
        verify(cb, times(1)).invoke(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f))
    }

    @Test
    fun flow_shouldEmit_WhenThreshold2() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(10)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        val job = launch {
            accumulator.flow
                .onEach(cb)
                .toList(results)
        }
        delay(1)
        accumulator.add(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f))
        accumulator.add(floatArrayOf(9f, 10f))
        accumulator.close()
        job.join()

        // Assert
        assertEquals(1, results.size)
        assertEquals(10, results[0].size)
        verify(cb, times(1)).invoke(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f))
    }

    @Test
    fun flow_shouldEmit_WhenAboveThreshold1() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(5)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        val job = launch {
            accumulator.flow
                .onEach(cb)
                .toList(results)
        }
        delay(10)
        accumulator.add(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f))
        accumulator.close()
        job.join()

        // Assert
        assertEquals(1, results.size)
        assertEquals(5, results[0].size)
        verify(cb, times(1)).invoke(floatArrayOf(1f, 2f, 3f, 4f, 5f))
    }

    @Test
    fun flow_shouldEmit_WhenAboveThreshold2() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(5)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        val job = launch {
            accumulator.flow
                .onEach(cb)
                .toList(results)
        }
        delay(1)
        accumulator.add(floatArrayOf(1f, 2f, 3f))
        accumulator.add(floatArrayOf(4f, 5f, 6f))
        accumulator.close()
        job.join()

        // Assert
        assertEquals(1, results.size)
        assertEquals(5, results[0].size)
        verify(cb, times(1)).invoke(floatArrayOf(1f, 2f, 3f, 4f, 5f))
    }

    @Test
    fun flow_shouldEmit2_WhenAboveThreshold2() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(5)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        val job = launch {
            accumulator.flow
                .onEach(cb)
                .toList(results)
        }
        delay(1)
        accumulator.add(floatArrayOf(1f, 2f, 3f))
        accumulator.add(floatArrayOf(4f, 5f, 6f))
        accumulator.add(floatArrayOf(7f, 8f, 9f))
        accumulator.add(floatArrayOf(10f, 11f, 12f))
        accumulator.close()
        job.join()

        // Assert
        assertEquals(2, results.size)
        assertEquals(5, results[0].size)
        assertEquals(5, results[1].size)
        val inOrder = inOrder(cb)
        inOrder.verify(cb, times(1)).invoke(floatArrayOf(1f, 2f, 3f, 4f, 5f))
        inOrder.verify(cb, times(1)).invoke(floatArrayOf(6f, 7f, 8f, 9f, 10f))
    }
}