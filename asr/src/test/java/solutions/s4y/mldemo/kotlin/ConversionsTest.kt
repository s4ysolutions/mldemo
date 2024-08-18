package solutions.s4y.mldemo.kotlin

import androidx.lifecycle.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import solutions.s4y.mldemo.kotlin.fixtures.Scopes
import solutions.s4y.mldemo.kotlin.fixtures.FlowExt
import solutions.s4y.mldemo.kotlin.fixtures.LongTasks
import solutions.s4y.mldemo.kotlin.fixtures.Randomizing
import solutions.s4y.mldemo.kotlin.fixtures.Reporting

class ConversionsTest : Reporting, Randomizing, LongTasks, Scopes, FlowExt {
    private fun worker(i: Int): Deferred<Double> =
        computeScope.async {
            println("worker $i enter")
            val x = longTask()
            println("worker $i done")
            x
        }.also { deferred ->
            deferred.invokeOnCompletion { err ->
                if (err == null) {
                    println("worker $i completed")
                } else {
                    println("worker $i canceled $err")
                }
            }
        }

    @Test
    @DisplayName("processing jobs dispatches data to MutableSharedFlow")
    fun mutableFlow_shouldProcess() = runBlocking {
        // Arrange
        val cb: (Int) -> Unit = mock()
        val origin = MutableSharedFlow<Int>()
        val producer = MutableSharedFlow<String>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        // Act
        println("processing coroutine create...")
        val prevJob = AtomicReference<Job?>(null)

        val jobProcessing = origin.onEach {
            val i = it
            println("enter processor $i prevJob=$prevJob")
            val job = worker(i)
            job.invokeOnCompletion { err ->
                if (err == null) {
                    println("invokeOnCompletion $i")
                    val t = producer.tryEmit("work $i")
                    println("try emit tp producer (${producer.subscriptionCount.value}) $i $t")
                } else {
                    println("invokeOnCompletion canceled job $i, $err")
                }
            }
            prevJob.getAndSet(job)?.let {
                println("cancel previous job $it")
                it.cancel()
            }
            println("exit processor $i job=$prevJob")
        }.launchIn(processScope)
        println("processing coroutine done...")
        println("launch collecting...")
        val resultsD = processScope.async {
            producer.take(1).toList()
        }
        println("launch collecting done")
        producer.waitSubscribers()
        println("emit 1")
        origin.emit(1)
        delay(5)
        println("emit 2")
        origin.emit(2)
        delay(5)
        println("emit 3")
        origin.emit(3)

        val results = resultsD.await()
        println("results: $results")
        jobProcessing.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun flatMapLatest_shouldProcess() = runBlocking {
        // Arrange
        val cb: (Int) -> Unit = mock()
        val origin = MutableSharedFlow<Int>()
        // Act
        println("processing flow create...")

        val processingFlow = origin.flatMapLatest {
            val i = it
            println("processor $i enter")
            flow {
                println("processor flow $i enter")
                val job = worker(i)
                println("processor $i launched job=$job")
                try {
                    job.await()
                    val e = "$i result"
                    println("processor flow $i emit $e")
                    emit(e)
                    println("processor flow $i exit")
                }catch (e: CancellationException) {
                    println("processor flow $i canceled")
                    job.cancel()
                }
            }
        }
        println("processing flow done...")

        println("launch collecting...")
        val resultsD = processScope.async {
            processingFlow.take(1).toList()
        }
        origin.waitSubscribers()
        println("launch collecting done")
        println("emit 1")
        origin.emit(1)
        println("emit 2")
        origin.emit(2)
        println("emit 3")
        origin.emit(3)

        val results = resultsD.await()
        println("results: $results")
        assertEquals(1, results.size)
        assertEquals("3 result", results[0])
    }
}