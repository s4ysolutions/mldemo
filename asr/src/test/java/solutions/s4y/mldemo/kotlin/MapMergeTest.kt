package solutions.s4y.mldemo.kotlin

import androidx.lifecycle.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.inOrder
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

var ts = System.currentTimeMillis()
val ts0 = System.currentTimeMillis()
fun println(msg: String) {
    kotlin.io.println("${System.currentTimeMillis() - ts0}:${System.currentTimeMillis() - ts}: $msg")
    ts = System.currentTimeMillis()
}

class MapMergeTest {
    private val random = Random(System.currentTimeMillis())
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @Disabled("This test is not working as expected")
    @Test
    fun mapMerge_shouldLaunch_inParallel() = runBlocking {
        println("start test ${Thread.currentThread()}")
        // Arrange
        println("create producer ${Thread.currentThread()}..")
        val producer = MutableSharedFlow<Int>(extraBufferCapacity = 1)
        println("create mock...")
        val cb: (String) -> Unit = mock()
        println("define  compute scope...")
        val computeScope = CoroutineScope(newSingleThreadContext("ComputeScope"))

        println("define  worker...")
        val worker = { it: Int ->
            computeScope.async {
                cb("worker enter $it")
                println("worker enter $it ${Thread.currentThread()}")
                var x = 0.0
                val ts = System.currentTimeMillis()
                for (i in 1..100000000) {
                    x += ln(sqrt(random.nextDouble()))
                }
                println("worker exit $it: ${System.currentTimeMillis() - ts}")
                cb("worker exit $it")
                "a$it:$x"
            }
        }

        println("create mapped")
        val prevJob = AtomicReference<Deferred<String>?>(null)
        val mapped = producer.flatMapMerge {
            val i = it
            println("flatMapMerge enter $i ${Thread.currentThread()}")
            callbackFlow {
                println("callbackFlow enter $i ${Thread.currentThread()}")
                val i = it
                val deferred = worker(it)
                println("set prevjob $i $deferred")
                val pj = prevJob.getAndSet(deferred)
                if (pj != null) {
                    println("kill prevjob from $i $pj")
                    pj.cancel()
                } else {
                    println("no prevjob $i")
                }
                println("callbackFlow wait for completion $i ${Thread.currentThread()} ...")
                deferred.invokeOnCompletion {
                    println("invokeOnCompletion enter $i ${Thread.currentThread()}")
                    trySend(it)
                    close()
                }
                awaitClose().also {
                    println("callbackFlow closed $i ${Thread.currentThread()}")
                }
                println("callbackFlow exit $i ${Thread.currentThread()}")
            }.also {
                println("flatMapMerge exit $i")
            }
        }


        // Act
        val deferred = CoroutineScope(Dispatchers.IO).async {
            println("collect enter...  ${Thread.currentThread()}")
            mapped.take(3).toList()
        }

        while (producer.subscriptionCount.value == 0) {
            println("waiting for subscription...  ${Thread.currentThread()}")
            delay(10)
        }

        println("emit 1  ${Thread.currentThread()}")
        producer.emit(1)
        println("emit 2")
        producer.emit(2)
        println("emit 3")
        producer.emit(3)
        println("emit 4")
        producer.emit(4)

        deferred.await()
        // Assert

        val inOrder = inOrder(cb)
        inOrder.verify(cb).invoke("worker enter 1")
        inOrder.verify(cb).invoke("worker enter 2")
        inOrder.verify(cb).invoke("worker exit 1")
        inOrder.verify(cb).invoke("worker exit 2")
    }
}