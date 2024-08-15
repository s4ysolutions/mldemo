package solutions.s4y.pcm

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import solutions.s4y.audio.accumulator.PCMFeed
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class PCMFeedTest {
    companion object {
        // denormalize
        fun d(shorts: ShortArray): ShortArray {
            return shorts.map { (Short.MAX_VALUE * it / 10).toShort() }.toShortArray()
        }

        fun r(floats: FloatArray): FloatArray {
            return floats.map { (it * 100 + 0.5).toInt() / 100f }.toFloatArray()
        }
    }

    @Nested
    inner class FlowTest {
        @Test
        fun flow_shouldNotEmit_WhenBelowBatch() = runBlocking {
            // Arrange
            val accumulator = PCMFeed(10)
            val results = mutableListOf<FloatArray>()
            val cb: (FloatArray) -> Unit = mock()
            // Act
            val job = launch {
                withTimeout(300) {
                    accumulator.flow
                        .take(1)
                        .onEach(cb)
                        .toList(results)
                }
            }
            delay(1)
            accumulator.add(shortArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
            //accumulator.close()
            job.join()

            // Assert
            assertTrue(results.isEmpty())
            verify(cb, never()).invoke(any())
        }

        @Test
        fun flow_shouldEmit_WhenBatch() = runBlocking {
            // Arrange
            val accumulator = PCMFeed(10)
            val results = mutableListOf<FloatArray>()
            val cb: (FloatArray) -> Unit = mock()
            // Act
            val job = launch {
                withTimeout(500) {
                    accumulator.flow
                        .map { r(it) }
                        .onEach(cb)
                        .toList(results)
                }
            }
            delay(1)
            accumulator.add(d(shortArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
            accumulator.close()
            job.join()

            // Assert
            assertEquals(1, results.size)
            assertEquals(10, results[0].size)
            verify(cb, times(1)).invoke(
                floatArrayOf(
                    .1f,
                    .2f,
                    .3f,
                    .4f,
                    .5f,
                    .6f,
                    .7f,
                    .8f,
                    .9f,
                    1.0f
                )
            )
        }

        @Test
        fun flow_shouldEmit_WhenBatch2() = runBlocking {
            // Arrange
            val accumulator = PCMFeed(10)
            val results = mutableListOf<FloatArray>()
            val cb: (FloatArray) -> Unit = mock()
            // Act
            val job = launch {
                withTimeout(200) {
                    accumulator.flow
                        .map { r(it) }
                        .onEach(cb)
                        .toList(results)
                }
            }
            delay(1)
            accumulator.add(d(shortArrayOf(1, 2, 3, 4, 5, 6, 7, 8)))
            accumulator.add(d(shortArrayOf(9, 10)))
            accumulator.close()
            job.join()

            // Assert
            assertEquals(1, results.size)
            assertEquals(10, results[0].size)
            verify(cb, times(1)).invoke(
                floatArrayOf(
                    .1f,
                    .2f,
                    .3f,
                    .4f,
                    .5f,
                    .6f,
                    .7f,
                    .8f,
                    .9f,
                    1.0f
                )
            )
        }

        @Test
        fun flow_shouldEmit_WhenAboveBatch1() = runBlocking {
            // Arrange
            val accumulator = PCMFeed(5)
            val results = mutableListOf<FloatArray>()
            val cb: (FloatArray) -> Unit = mock()
            // Act
            val job = launch {
                withTimeout(200) {
                    accumulator.flow
                        .take(6)
                        .map { r(it) }
                        .onEach(cb)
                        .toList(results)
                }
            }
            delay(1)
            accumulator.add(d(shortArrayOf(1, 2, 3, 4, 5, 6)))
            accumulator.close()
            job.join()

            // Assert
            assertEquals(1, results.size)
            assertEquals(5, results[0].size)
            verify(cb, times(1)).invoke(floatArrayOf(.1f, .2f, .3f, .4f, .5f))
        }

        @Test
        fun flow_shouldEmit_WhenAboveThreshold2() = runBlocking {
            // Arrange
            val accumulator = PCMFeed(5)
            val results = mutableListOf<FloatArray>()
            val cb: (FloatArray) -> Unit = mock()
            // Act
            val job = launch {
                withTimeout(200) {
                    accumulator.flow
                        .map { r(it) }
                        .onEach(cb)
                        .toList(results)
                }
            }
            delay(1)
            accumulator.add(d(shortArrayOf(1, 2, 3)))
            accumulator.add(d(shortArrayOf(4, 5, 6)))
            accumulator.close()
            job.join()

            // Assert
            assertEquals(1, results.size)
            assertEquals(5, results[0].size)
            verify(cb, times(1)).invoke(floatArrayOf(.1f, .2f, .3f, .4f, .5f))
        }

        @OptIn(FlowPreview::class)
        @Test
        fun flow_shouldEmit2_WhenAboveThreshold2() = runBlocking {
            // Arrange
            val accumulator = PCMFeed(3)
            val results = mutableListOf<FloatArray>()
            val cb: (FloatArray) -> Unit = mock()
            // Act
            var launched = false
            val job = CoroutineScope(Dispatchers.IO).launch {
                accumulator.flow
                    .onSubscription { launched = true }
                    .timeout(1000.toDuration(DurationUnit.MILLISECONDS))
                    .catch { emit(FloatArray(0)) }
                    .takeWhile { it.isNotEmpty() }
                    .map { r(it) }
                    .onEach(cb)
                    .toList(results)
            }
            while (launched.not()) delay(5)
            accumulator.add(d(shortArrayOf(1, 2)))
            accumulator.add(d(shortArrayOf(3, 4)))
            accumulator.add(d(shortArrayOf(5, 6)))
            accumulator.add(d(shortArrayOf(7, 8)))
            while (accumulator.flush()) delay(5)
            job.join()

            // Assert
            assertEquals(2, results.size)
            assertEquals(3, results[0].size)
            assertEquals(3, results[1].size)
            val inOrder = inOrder(cb)
            inOrder.verify(cb, times(1)).invoke(floatArrayOf(.1f, .2f, .3f))
            inOrder.verify(cb, times(1)).invoke(floatArrayOf(.4f, .5f, .6f))
        }

        @OptIn(FlowPreview::class)
        @Test
        fun flow_shouldEmit_When282624() = runBlocking {
            // Arrange
            val accumulator = PCMFeed(16 * 3000)
            val results = mutableListOf<FloatArray>()
            val cb: (FloatArray) -> Unit = mock()
            // Act
            val job = CoroutineScope(Dispatchers.IO).launch {
                accumulator.flow
                    .map {
                        r(it)
                    }
                    .timeout(2000.toDuration(DurationUnit.MILLISECONDS))
                    .catch { emit(FloatArray(0)) }
                    .takeWhile { it.isNotEmpty() }
                    .onEach(cb)
                    .toList(results)
            }
            delay(10)
            val shorts = ShortArray(282624) { it.toShort() }
            accumulator.add(shorts)
            accumulator.close()
            job.join()

            // Assert
            assertEquals(5, results.size)
            results.forEach {
                assertEquals(16 * 3000, it.size)
            }
            verify(cb, times(5)).invoke(any())
        }
    }

    @Nested
    inner class FlushTest {
        @OptIn(FlowPreview::class)
        @Test
        fun flush_shouldHandleLastBatch(): Unit = runBlocking {
            val accumulator = PCMFeed(2)

            val results = mutableListOf<FloatArray>()
            val cb: (FloatArray) -> Unit = mock()
            var launched = AtomicBoolean(false)
            val job = CoroutineScope(Dispatchers.Default).launch {
                accumulator.flow
                    .onSubscription { launched.set(true) }
                    .timeout(500.toDuration(DurationUnit.MILLISECONDS)).catch { throw CancellationException() }
                    // .onEach { println(it.joinToString(prefix = "onEach:")) }
                    .map { r(it) }
                    .onEach(cb)
                    .toList(results)
            }
            while (!launched.get()) delay(5)
            accumulator.add(d(shortArrayOf(1, 2, 3, 4)))
            while (accumulator.flush()) delay(1)

            // accumulator.flush()
            job.join()
            verify(cb, times(2)).invoke(any())
            val inorder = inOrder(cb)
            inorder.verify(cb, times(1)).invoke(floatArrayOf(.1f, .2f))
            inorder.verify(cb, times(1)).invoke(floatArrayOf(.3f, .4f))
            assertEquals(2, results.size)
        }
    }
}