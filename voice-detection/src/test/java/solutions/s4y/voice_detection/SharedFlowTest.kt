package solutions.s4y.voice_detection

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class SharedFlowTest {
    @Test
    fun map_shouldRunForEverySubscriber() = runBlocking {
        val sf: MutableSharedFlow<Int> = MutableSharedFlow()
        val cb: (Int) -> Unit = mock()
        val f = sf.map {
            cb(it)
            it
        }
        val job1 = launch {
            f.take(1).collect {
                println("job1: $it")
            }
        }

        val job2 = launch {
            f.take(1).collect {
                println("job2: $it")
            }
        }

        delay(10)
        sf.emit(1)

        joinAll(job1, job2)

        verify(cb, times(2)).invoke(1)
    }

    @Test
    fun transform_shouldRunForEverySubscriber() = runBlocking {
        val sf: MutableSharedFlow<Int> = MutableSharedFlow()
        val cb: (Int) -> Unit = mock()
        val f = sf.transform {
            cb(it)
            emit(it)
        }
        val job1 = launch {
            f.take(1).collect {
                println("job1: $it")
            }
        }

        val job2 = launch {
            f.take(1).collect {
                println("job2: $it")
            }
        }

        delay(10)
        sf.emit(1)

        joinAll(job1, job2)

        verify(cb, times(2)).invoke(1)
    }
}