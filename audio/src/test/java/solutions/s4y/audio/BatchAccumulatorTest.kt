package solutions.s4y.audio

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import solutions.s4y.audio.batch.BatchAccumulator

class BatchAccumulatorTest {
    @Test
    fun content_shouldReturnBatchedContent_whenEmpty() = runBlocking {
        // Arrange
        val accumulator = BatchAccumulator(3)

        // Act
        val content = accumulator.content()

        // Assert
        assertArrayEquals(floatArrayOf(), content)
    }

    @Test
    fun content_shouldReturnBatchedContent_whenNoBatches() = runBlocking {
        // Arrange
        val accumulator = BatchAccumulator(3)

        // Act
        accumulator.add(floatArrayOf(1f, 2f))
        val content = accumulator.content()

        // Assert
        assertArrayEquals(floatArrayOf(1f, 2f), content)
    }

    @Test
    fun content_shouldReturnBatchedContent_whenOneBatch() = runBlocking {
        // Arrange
        val accumulator = BatchAccumulator(3)

        // Act
        accumulator.add(floatArrayOf(1f, 2f, 3f))
        val content = accumulator.content()

        // Assert
        assertArrayEquals(floatArrayOf(1f, 2f, 3f), content)
    }

    @Test
    fun content_shouldReturnBatchedContent_whenOneAndHalfBatch() = runBlocking {
        // Arrange
        val accumulator = BatchAccumulator(3)

        // Act
        accumulator.add(floatArrayOf(1f, 2f, 3f))
        accumulator.add(floatArrayOf(4f))
        val content = accumulator.content()

        // Assert
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f), content)
    }

    @Test
    fun content_shouldReturnBatchedContent_whenTwoBatches() = runBlocking {
        // Arrange
        val accumulator = BatchAccumulator(3)

        // Act
        accumulator.add(floatArrayOf(1f, 2f, 3f))
        accumulator.add(floatArrayOf(4f, 5f, 6f))
        val content = accumulator.content()

        // Assert
        assertArrayEquals(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f), content)
    }
}