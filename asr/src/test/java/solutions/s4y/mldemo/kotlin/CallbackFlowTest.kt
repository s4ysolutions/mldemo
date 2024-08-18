package solutions.s4y.mldemo.kotlin

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.inOrder

class CallbackFlowTest {
    @Test
    fun callbackFlowTest_shouldNotWait() = runBlocking {
        // Arrange
        var emit: (Int) -> Unit = { }
        var close: () -> Unit = { }
        var cb: (Int) -> Unit = mock()

        val flow = callbackFlow {
            emit = {
                trySend(it)
            }
            close = {
                close()
            }
            cb(1)
            awaitClose()
            cb(2)
        }
        cb(3)
        // Act
        // Assert
        val inOrder = inOrder(cb)
    }
}