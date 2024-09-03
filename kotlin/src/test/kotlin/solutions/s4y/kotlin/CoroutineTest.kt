package solutions.s4y.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import kotlin.coroutines.CoroutineContext

class CoroutineTest {
    @Test
    fun scope_shouldNotCreateCoroutine_whenItsJobCancelled() = runBlocking {
        // Arrange
        val scope = CoroutineScope(Dispatchers.Default + Job())
        scope.coroutineContext[Job]?.cancel()
        val launchBody: suspend () -> Unit = mock()
        // Act
        val job = scope.launch {
            launchBody()
        }
        job.join()
        yield()
        // Assert

        verify(launchBody, timeout(1000).times(0)).invoke()
    }

    @Suppress("USELESS_IS_CHECK")
    @Test
    fun jobs_shouldBeOfSameClass_whenAssociatedWithScopeAndCoroutines() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val scopeJob = scope.coroutineContext[Job]
        val coroutineJob = scope.launch {}
        coroutineJob.join()
        if (scopeJob == null) {
            throw AssertionError("scopeJob is null")
        }
        assertNotEquals(scopeJob::class.java, coroutineJob::class.java)
        assertTrue(scopeJob is Job)
        assertTrue(coroutineJob is Job)
    }

    @Test
    fun coroutine_shouldReferenceScopeContext() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val scopeContext = scope.coroutineContext
        val scopeJob = scope.coroutineContext[Job]
        var launchedCoroutineContext: CoroutineContext? = null
        var launchedCoroutineJob: Job? = null
        val coroutineJob = scope.launch {
            launchedCoroutineContext = coroutineContext
            launchedCoroutineJob = coroutineContext[Job]
        }

        coroutineJob.join()

        assertNotSame(scopeContext, launchedCoroutineContext)
        assertSame(coroutineJob, launchedCoroutineJob)
        assertNotSame(coroutineJob, scopeJob)
    }
}