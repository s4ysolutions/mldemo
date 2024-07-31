package solutions.s4y.pcm

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class PCMFeedTest {
    @Test
    fun waveForms_shouldEmitsFloatArray(): Unit = runBlocking {
        // Arrange
        val cb: (FloatArray) -> Unit = mock()
        val pcmFeed: IPCMFeed = PCMFeed()
        pcmFeed.batch = 10
        val job = pcmFeed.waveForms
            .onEach(cb)
            .launchIn(this)
        // Act
        pcmFeed.addSamples(shortArrayOf(6, 7, 8, 9, -10))
        pcmFeed.addSamples(shortArrayOf(3, 4, 5))
        pcmFeed.addSamples(shortArrayOf(2, 1, 11))
        // Assert
        delay(10)
        // timeout(1000) blocks the coroutine until the timeout is reached
        // it is not suspended
        verify(cb, times(1)).invoke(
            floatArrayOf(
                0.6f,
                0.7f,
                0.8f,
                0.9f,
                -1.0f,
                0.3f,
                0.4f,
                0.5f,
                0.2f,
                0.1f
            )
        )
        job.cancel()
    }
}