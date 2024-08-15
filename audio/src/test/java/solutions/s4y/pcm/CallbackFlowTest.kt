package solutions.s4y.pcm

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify


class CallbackFlowTest {
    @Test
    fun callbackFlow_canBeClosed(): Unit = runBlocking {
        val f = callbackFlow<Int> {
            send(1)
            send(2)
            send(3)
            close()
        }
        val l = mutableListOf<Int>()
        f.toList(l)
        assertEquals(listOf(1, 2, 3), l)
    }

    class CbManager {
        private var cb: (Int) -> Unit = {}
        private var _close: () -> Unit = {}
        fun setCb(cb: (Int) -> Unit) {
            this.cb = cb
        }

        fun send(i: Int) {
            cb(i)
        }

        fun setClose(close: () -> Unit) {
            this._close = close
        }

        fun close() {
            _close()
        }
    }

    @Test
    fun callbackFlow_canBeClosedFromCallback(): Unit = runBlocking {
        // Arrange
        val cbManager = CbManager()
        val f = callbackFlow<Int> {
            cbManager.setCb { trySend(it) }
            cbManager.setClose { close() }
            awaitClose()
        }
        // Act
        val l = mutableListOf<Int>()
        val job = launch {
            f.toList(l)
        }

        delay(10)
        cbManager.send(1)
        cbManager.send(2)
        cbManager.send(3)
        cbManager.close()
        job.join()

        // Assert

        assertEquals(listOf(1, 2, 3), l)
    }

    @Disabled("It it not important but takes the time")
    @Test
    fun callbackFlow_shouldBeCalledOnce(): Unit = runBlocking {
        val cbCallbackFlow: () -> Unit = mock()
        val cbCollect: (List<Int>) -> Unit = mock()

        // Arrange
        val cbManager = CbManager()
        val f = callbackFlow<Int> {
            cbCallbackFlow()
            cbManager.setCb { trySend(it) }
            cbManager.setClose {
                close()
            }
            awaitClose()
        }
        // Act
        val job1 = async {
            val l = mutableListOf<Int>()
            f.toList(l)
            cbCollect(l)
            l
        }
        delay(10)
        cbManager.send(1)

        val job2 = async {
            val l = mutableListOf<Int>()
            f.toList(l)
            cbCollect(l)
            l
        }
        delay(10)
        cbManager.send(2)

        delay(10)
        cbManager.close()
        val results = try {
            withTimeout(300) {
                awaitAll(job1, job2)
            }
        } catch (_: TimeoutCancellationException) {
            job1.cancel()
            job2.cancel()
            emptyList<Int>()
        }
        // Assert

        // because 2 times toList
        verify(cbCallbackFlow, times(2)).invoke()
        // 1 was sent but close was overridden and never sent
        verify(cbCollect, never()).invoke(listOf(1))
        verify(cbCollect).invoke(listOf(2))
        assertEquals(emptyList<Any>(), results)
    }
}