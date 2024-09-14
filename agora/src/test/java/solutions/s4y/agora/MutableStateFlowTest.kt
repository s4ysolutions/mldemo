package solutions.s4y.agora

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class MutableStateFlowTest {
    @Test
    fun onEach_shouldBeTriggered_WhenInit() = runBlocking {
        // Arrange
        val cbOnEach: (Int) -> Unit = mock()
        val cbOnCollect: (Int) -> Unit = mock()
        // Act
        MutableStateFlow(0).onEach { cbOnEach(it) }.take(1).collect { cbOnCollect(it) }
        // Assert
        verify(cbOnEach).invoke(0)
        verify(cbOnCollect).invoke(0)
    }

    @Test
    fun onEach_shouldBeTriggered_IfNotCollecting() = runBlocking {
        // Arrange
        val cbOnCollect: (Int) -> Unit = mock()
        val flow = MutableStateFlow(0)
        // Act
        val job = launch {
            flow.take(1).collect { cbOnCollect(it) }
        }
        flow.value = 1
        job.join()
        // Assert
        verify(cbOnCollect).invoke(1)
        verify(cbOnCollect, never()).invoke(0)
    }
}