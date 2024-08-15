package solutions.s4y.mldemo.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SharedFlowTest {
    @Test
    fun sharedFlow_shouldRestart_whenResubscribed() = runBlocking {
        // Arrange
        val scope = CoroutineScope(Dispatchers.IO)
        val hotFlow = MutableSharedFlow<Int>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val productFlow = hotFlow
            .onEach {
                println("p=$it")
            }
            .shareIn(scope, started = SharingStarted.WhileSubscribed())
        // Act
        delay(10)
        hotFlow.tryEmit(1)
        delay(10)

        val result1 = CoroutineScope(Dispatchers.Default).async {
            println("subscription 1")
            productFlow
                .onEach {
                    println("f1=$it")
                }
                .takeWhile { it < 4 }
                .toList()
                .also {
                    println("subscription 1 done")
                }
        }

        delay(100)
        hotFlow.tryEmit(2)

        delay(10)
        hotFlow.tryEmit(3)

        delay(10)
        hotFlow.tryEmit(4)

        val result2 = async {
            println("subscription 2")
            productFlow
                .onEach {
                    println("f2=$it")
                }
                .takeWhile { it < 7 }
                .toList()
                .also {
                    println("subscription 2 done")
                }
        }

        delay(10)
        hotFlow.tryEmit(5)

        delay(10)
        hotFlow.tryEmit(6)

        delay(10)
        hotFlow.tryEmit(7)

        delay(1000)
        val results = awaitAll(result1, result2)
        // Assert
        assertEquals(listOf(2, 3), results[0])
        assertEquals(listOf(5, 6), results[1])
    }
}