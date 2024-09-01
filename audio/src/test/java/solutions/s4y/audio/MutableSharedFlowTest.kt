package solutions.s4y.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MutableSharedFlowTest {
    @Test
    fun tryEmit_shouldBeTrue_whenNoSubscribers() {
        val mutableSharedFlow = MutableSharedFlow<Int>()
        assertTrue(mutableSharedFlow.tryEmit(1))
    }

    @OptIn(FlowPreview::class)
    @Test
    fun tryEmit_shouldBeFalse_whenSubscriberReadyButNotSuspend() = runBlocking {
        val cb: (Int) -> Unit = mock()
        val mutableSharedFlow = MutableSharedFlow<Int>()

        val job = CoroutineScope(Dispatchers.Default).launch {
            mutableSharedFlow
                .timeout(1000.toDuration(DurationUnit.MILLISECONDS))
                .collect {
                    println("collect = $it")
                    cb(it)
                }
        }
        delay(200)
        val t = mutableSharedFlow.tryEmit(1)
        job.join()

        assertFalse(t)
        verify(cb, never()).invoke(any())
    }

    @OptIn(FlowPreview::class)
    @Test
    fun tryEmit_shouldBeTrueAndFalse_whenSubscriberReadyAndSuspend() = runBlocking {
        val cb: (Int) -> Unit = mock()
        val mutableSharedFlow = MutableSharedFlow<Int>(onBufferOverflow = BufferOverflow.SUSPEND, extraBufferCapacity = 1)

        val job = CoroutineScope(Dispatchers.Default).launch {
            mutableSharedFlow
                .timeout(300.toDuration(DurationUnit.MILLISECONDS))
                .collect {
                    println("collect = $it")
                    cb(it)
                }
        }

        while (mutableSharedFlow.subscriptionCount.value == 0)
            delay(5)

        val t1 = mutableSharedFlow.tryEmit(1)
        val t2 = mutableSharedFlow.tryEmit(2)
        job.join()

        assertTrue(t1)
        assertFalse(t2)
        verify(cb, times(1)).invoke(1)
    }

    @OptIn(FlowPreview::class)
    @Test
    fun tryEmit_shouldBeTrueAndTrue_whenSubscriberReadyAndSuspend() = runBlocking {
        val cb: (Int) -> Unit = mock()
        val mutableSharedFlow = MutableSharedFlow<Int>(onBufferOverflow = BufferOverflow.SUSPEND, extraBufferCapacity = 1)

        val job = CoroutineScope(Dispatchers.Default).launch {
            mutableSharedFlow
                .timeout(300.toDuration(DurationUnit.MILLISECONDS))
                .collect {
                    println("collect = $it")
                    cb(it)
                }
        }

        while (mutableSharedFlow.subscriptionCount.value == 0)
            delay(5)

        val t1 = mutableSharedFlow.tryEmit(1)
        // let the buffer be consumed
        delay(5)
        val t2 = mutableSharedFlow.tryEmit(2)
        job.join()

        assertTrue(t1)
        assertTrue(t2)
        verify(cb, times(1)).invoke(1)
    }
}