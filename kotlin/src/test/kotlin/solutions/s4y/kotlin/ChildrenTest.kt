package solutions.s4y.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.mockingDetails
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class ChildrenTest {

    fun arrange_coroutines(
        parentScope: CoroutineScope,
        onTest: (
            childScope: CoroutineScope,
            flowJob: Job,
        ) -> Unit
    ): Pair<Int, List<() -> Unit>> {
        // Arrange
        val childJob = Job(parentScope.coroutineContext[Job])
        val childScope = CoroutineScope(parentScope.coroutineContext + childJob)

        var flowEmitsCount = 0
        val flowExit: () -> Unit = mock()

        val f = flow {
            try {
                while (true) {
                    println("parent is running")
                    emit(1)
                    flowEmitsCount++
                    delay(100)
                }
            } finally {
                println("parent is canceled")
                flowExit()
            }
        }

        val onEachLaunch: () -> Unit = mock()
        val onEachExit: () -> Unit = mock()

        // Act
        val flowJob = f.onEach {
            childScope.launch {
                try {
                    while (true) {
                        println("child is running")
                        delay(100)
                    }
                } finally {
                    println("child is canceled")
                    onEachExit()
                }
            }
            onEachLaunch()
        }.launchIn(parentScope)


        runBlocking {
            delay(300)
            println("cancel parent")
            onTest(childScope, flowJob)
            flowJob.join()
            delay(200)
            println("exit")
        }
        return Pair(flowEmitsCount, listOf(flowExit, onEachLaunch, onEachExit))
    }

    /**
     * This test demonstrates that a child coroutines are canceled when
     * the parent scope is canceled.
     */
    @Test
    fun childrenCoroutines_shouldBeCanceled_whenParentIsCanceled() {
        val parentScope = CoroutineScope(Dispatchers.Default)
        val (flowEmitsCount, mocks) = arrange_coroutines(parentScope) { _, _ ->
            parentScope.cancel()
        }
        val (flowExit, onEachLaunch, onEachExit) = mocks
        verify(flowExit).invoke()
        verify(onEachLaunch, times(flowEmitsCount)).invoke()
        verify(onEachExit, times(flowEmitsCount)).invoke()
    }

    /**
     * This test demonstrates that a child coroutines are not canceled when
     * the child job is canceled.
     */
    fun child_shouldBeCanceled_whenChildIsCanceled() {
        val parentScope = CoroutineScope(Dispatchers.Default)
        val (flowEmitsCount, mocks) = arrange_coroutines(parentScope) { childScope, flowJob ->
            parentScope.cancel()
        }
    }

}