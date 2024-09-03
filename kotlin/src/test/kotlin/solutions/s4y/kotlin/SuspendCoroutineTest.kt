package solutions.s4y.kotlin

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuspendCoroutineTest {
    private val runDispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()
    private var runContext: CoroutineContext = runDispatcher

    @Test
    fun suspendCoroutine_shouldRunThread_AfterContinuationResume() = runBlocking(runContext) {
        val cb: (Int) -> Unit = mock()
        cb(0)
        println("0 launch coroutine")
        var thread: Thread? = null
        val job = launch() {
            delay(50) //let "8 join" run first
            cb(1)
            println("1 lanuched coroutine entered")
            suspendCoroutine<Unit> { continuation ->
                cb(2)
                println("2 suspendCoroutine enter and start thread")
                thread = Thread {
                    println("3 thread starts and sleeps")
                    cb(3)
                    Thread.sleep(200)
                    cb(4)
                    println("4 thread awakes and resumes continuation")
                    continuation.resume(Unit) // this makes job to be joined
                    cb(45)
                    println("45 thread resumed continuation and sleeps again")
                    Thread.sleep(100) // make sure emitted value would have had the time
                    cb(5)
                    println("5 thread is about to end")
                }
                thread?.start()
                println("6 suspendCoroutine exit while thread is running")
                cb(6)
            }
            cb(7)
            println("7 launched coroutine is about to exit")
        }
        cb(8)
        println("8 wait for coroutine to join")
        job.join()
        cb(9)
        println("9 joined")
        thread?.join() // let the thread to end
        val order = inOrder(cb)
        order.verify(cb).invoke(0)
        order.verify(cb).invoke(8)
        order.verify(cb).invoke(1)
        order.verify(cb).invoke(2)
        order.verify(cb).invoke(6)
        order.verify(cb).invoke(3)
        order.verify(cb).invoke(4)
        order.verify(cb).invoke(45)
        order.verify(cb).invoke(7)
        order.verify(cb).invoke(9)
        order.verify(cb).invoke(5)
    }
}