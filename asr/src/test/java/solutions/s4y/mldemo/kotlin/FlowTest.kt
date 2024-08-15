package solutions.s4y.mldemo.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class FlowTest {
    companion object {
        var ts: Long = 0
        fun log(msg: String) {
            println("${System.currentTimeMillis() - ts} - [${Thread.currentThread().name}]: ${msg}")
        }

        @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
        val twoThreadDispatcher = newFixedThreadPoolContext(2, "TwoThreadPool")

        val productScope = CoroutineScope(Dispatchers.IO)
        val computerScope = CoroutineScope(twoThreadDispatcher)

        var longComputeOperationRunning = false

        private fun longComputeOperation(
            n: Int
        ): Float {
            longComputeOperationRunning = true
            val t0 = System.currentTimeMillis()
            log("enter long operation $n")
            try {
                var x = 0f
                for (i in 1..750000000) {
                    x += sqrt(Random.nextFloat())
                }
                return x;
            } finally {
                val t1 = System.currentTimeMillis()
                log("exit long opertation $n: (${t1 - t0})")
                longComputeOperationRunning = false
            }
        }
    }

    @Nested
    inner class MapMergeTest {

        private lateinit var _emit: (Int) -> Unit
        private lateinit var _close: () -> Unit

        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun mapMerge_shouldRunInBackground() {
            // Arrange
            val hotFlow = callbackFlow {
                _emit = {
                    log("try send $it")
                    val t = trySend(it)
                    log("sent success: ${t.isSuccess}")
                }
                _close = { close() }
                awaitClose()
            }


            val productFlow = hotFlow
                .filter {
                    log("filter $it")
                    if (longComputeOperationRunning)
                        log("skip long operation $it")
                    !longComputeOperationRunning
                }.flatMapMerge {
                    val n = it
                    log("flow long operation create $n")
                    flow {
                        computerScope.async { longComputeOperation(n) }.await()
                        emit(n.toFloat()).also {
                            log("flow long operation created $n")
                        }
                    }
                }

            // Act
            val job = productScope.async {
                val list = mutableListOf<Float>()
                productFlow
                    .collect {
                        log("productFlow: $it")
                        list.add(it)
                    }
                list
            }

            ts = System.currentTimeMillis()
            val result = runBlocking {
                delay(50)
                _emit(1)
                delay(700)
                _emit(2)
                delay(700)
                _emit(3)
                delay(700)
                _emit(4)
                delay(700)
                _emit(5)
                delay(700)
                _emit(6)
                delay(700)
                _close()

                job.await()
            }
            // Assert
            assertEquals(listOf(1f, 3f, 5f), result)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun mapMerge_ShouldEmitFewTimes() {
            // Arrange
            val hotFlow = flowOf(1)
            val productFlow = hotFlow
                .flatMapMerge {
                    flow {
                        emit(it)
                        for (i in 1..3) {
                            emit(i)
                        }
                    }
                }
            // Act
            val result = runBlocking {
                productFlow.toList()
            }
            // Assert
            assertEquals(listOf(1, 1, 2, 3), result)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun mapMerge_ShouldInstantiate_whenSubscribed() = runBlocking {
            var n = 1.0
            // Arrange
            val hotFlow = flow {
                val nn = n++
                delay(50)
                emit(10.0.pow(nn) + 1)
                delay(50)
                emit(10.0.pow(nn) + 2)
                delay(50)
                emit(10.0.pow(nn) + 3)
            }
            val product = hotFlow.flatMapMerge {
                flow {
                    // each subscriber should get a different value
                    delay(25)
                    emit(it.toInt())
                }
            }
            // Act
            val def1 = async {
                // 1st subscriber
                product.toList()
            }
            val def2 = async {
                // 2nd subscriber
                product.toList()
            }

            // wait for both subscribers to finish
            val results = awaitAll(def1, def2)
            // Assert
            assertEquals(listOf(11, 12, 13, 101, 102, 103), results.flatten().sorted())
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        @Test
        fun mapMergeShared_ShouldInstantiateOnce_whenSubscribed() = runBlocking {
            var n = 1.0
            // Arrange
            val hotFlow = flow {
                val nn = n++
                delay(50)
                emit(10.0.pow(nn) + 1)
                delay(50)
                emit(10.0.pow(nn) + 2)
                delay(50)
                emit(10.0.pow(nn) + 3)
            }
            val product = hotFlow.flatMapMerge {
                flow {
                    // each subscriber should get a different value
                    delay(25)
                    emit(it.toInt())
                }
            }.shareIn(
                CoroutineScope(Dispatchers.IO),
                replay = 1,
                started = SharingStarted.WhileSubscribed()
            )
            // Act
            val def1 = async {
                product
                    .take(3)
                    .toList()
            }
            val def2 = async {
                product
                    .take(3)
                    .toList()
            }

            // wait for both subscribers to finish
            val results = awaitAll(def1, def2)
            // Assert
            assertEquals(listOf(11, 12, 13), results[0])
            assertEquals(listOf(11, 12, 13), results[1])
        }
    }
}