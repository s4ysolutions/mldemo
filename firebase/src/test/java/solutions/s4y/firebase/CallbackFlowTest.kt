package solutions.s4y.firebase

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class CallbackFlowTest {
    @Test
    fun callbackFlow_shouldBeCollected() = runBlocking {
        // Arrange
        val delayedOp = { f: (Int) -> Unit ->
            println("start delayedOp")
            Thread.sleep(100)
            println("exit delayedOp")
            f(42)
        }
        val f = callbackFlow {
            println("start callbackFlow")
            delayedOp { i ->
                trySend(i)
                close()
            }
            println("end callbackFlow")
            awaitClose()
        }
        val collectCB: (Int) -> Unit = mock()
        // Act
        println("start collect")
        f.collect(collectCB)
        println("end collect")
        verify(collectCB).invoke(42)
    }

    @Test
    fun callbackFlow_shouldTrow_whenThrown() {
        // Arrange
        val delayedOp = { f: (Int) -> Unit ->
            println("start delayedOp")
            Thread.sleep(100)
            println("exit delayedOp")
            f(42)
        }
        val ex = Exception("test")
        val f = callbackFlow<Int> {
            println("start callbackFlow")
            delayedOp {
                close(ex)
            }
            println("end callbackFlow")
            awaitClose()
        }
        val collectCB: (Int) -> Unit = mock()
        // Act
        assertThrows<Exception> {
            runBlocking {
                println("start collect")
                f.collect(collectCB)
                println("end collect")
            }
        }.also {
            assertEquals("test", it.message)
        }
        verify(collectCB, never()).invoke(any())
    }
}