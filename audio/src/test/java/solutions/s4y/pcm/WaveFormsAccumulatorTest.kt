package solutions.s4y.pcm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class WaveFormsAccumulatorTest {
    @OptIn(FlowPreview::class)
    @Test
    fun flow_shouldNotEmit_WhenBelowBatch() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(10)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        var launched = false
        val job = CoroutineScope(Dispatchers.Default).launch {
                accumulator.flow
                    .onSubscription { launched = true }
                    .timeout(300.toDuration(DurationUnit.MILLISECONDS)).catch { emit(FloatArray(0)) }
                    .takeWhile { it.isNotEmpty() }
                    .onEach(cb)
                    .toList(results)
        }
        while (launched.not()) delay(5)
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
            withTimeout(300) {
                accumulator.flow
                    .onEach(cb)
                    .toList(results)
            }
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

    @OptIn(FlowPreview::class)
    @Test
    fun flow_shouldEmit_WhenThreshold2() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(10)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        var launched = false
        val job = CoroutineScope(Dispatchers.Default).launch {
            accumulator.flow
                .onSubscription { launched = true }
                .timeout(300.toDuration(DurationUnit.MILLISECONDS)).catch { emit(FloatArray(0)) }
                .takeWhile { it.isNotEmpty() }
                .onEach(cb)
                .toList(results)
        }
        while (launched.not()) delay(5)
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
            withTimeout(300) {
                accumulator.flow
                    .onEach(cb)
                    .toList(results)
            }
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

    @OptIn(FlowPreview::class)
    @Test
    fun flow_shouldEmit_WhenAboveThreshold2() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(5)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        val ready = AtomicBoolean(false)
        val job = CoroutineScope(Dispatchers.IO).launch {
            accumulator.flow
                .onSubscription { ready.set(true) }
                .timeout(300.toDuration(DurationUnit.MILLISECONDS)).catch { emit(FloatArray(0)) }
                .takeWhile { it.isNotEmpty() }
                .onEach(cb)
                .toList(results)
        }
        while (ready.get().not()) delay(5);
        delay(5)
        accumulator.add(floatArrayOf(1f, 2f, 3f))
        delay(5)
        accumulator.add(floatArrayOf(4f, 5f, 6f))
        delay(5)
        accumulator.close()
        job.join()

        // Assert
        assertEquals(1, results.size)
        assertEquals(5, results[0].size)
        verify(cb, times(1)).invoke(floatArrayOf(1f, 2f, 3f, 4f, 5f))
    }

    @OptIn(FlowPreview::class)
    @Test
    fun flow_shouldEmit2_WhenAboveThreshold2() = runBlocking {
        // Arrange
        val accumulator = WaveFormsAccumulator(5)
        val results = mutableListOf<FloatArray>()
        val cb: (FloatArray) -> Unit = mock()
        // Act
        var launched = false
        val job = CoroutineScope(Dispatchers.Default).launch {
                accumulator.flow
                    .onSubscription { launched = true }
                    .timeout(300.toDuration(DurationUnit.MILLISECONDS)).catch { emit(FloatArray(0)) }
                    .takeWhile { it.isNotEmpty() }
                    .onEach(cb)
                    .toList(results)
        }
        while (launched.not()) delay(5)
        accumulator.add(floatArrayOf(1f, 2f, 3f))
        accumulator.add(floatArrayOf(4f, 5f, 6f))
        accumulator.add(floatArrayOf(7f, 8f, 9f))
        accumulator.add(floatArrayOf(10f, 11f, 12f))
        accumulator.flush()
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