package solutions.s4y.mldemo.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.inOrder
import solutions.s4y.mldemo.kotlin.fixtures.LongTasks
import solutions.s4y.mldemo.kotlin.fixtures.Randomizing
import solutions.s4y.mldemo.kotlin.fixtures.Reporting
import kotlin.math.ln
import kotlin.math.sqrt

class CoroutineTest : Reporting, Randomizing, LongTasks {
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val computeScope = CoroutineScope(newSingleThreadContext("ComputeScope"))

    @Nested
    inner class CancelTest {
        @Test
        fun launch_shouldBeCanceled() = runBlocking {
            // Arrange
            val cb: (Int) -> Unit = mock()
            // Act
            cb(1)
            println("coroutine start ${Thread.currentThread()} ...")
            val job = computeScope.launch {
                cb(2)
                println("coroutine enter ${Thread.currentThread()}")
                longTask()
                cb(3)
                println("coroutine exit ${Thread.currentThread()}")
            }
            println("coroutine launched ${Thread.currentThread()}")
            cb(4)
            delay(100)
            cb(5)
            println("coroutine cancel ${Thread.currentThread()} ...")
            cb(4)
            job.cancel()
            println("coroutine canceled ${Thread.currentThread()}")
            cb(6)
            // Assert
            val inOrder = inOrder(cb)
            inOrder.verify(cb).invoke(1)
            inOrder.verify(cb).invoke(4)
            inOrder.verify(cb).invoke(2)
            inOrder.verify(cb).invoke(5)
            inOrder.verify(cb).invoke(6)
            inOrder.verifyNoMoreInteractions()
        }
    }
}