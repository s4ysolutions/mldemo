package solutions.s4y.mldemo.asr.service.accumulator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import solutions.s4y.audio.accumulator.WaveFormsAccumulator
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class GrowingAccumulatorTest {
    @OptIn(FlowPreview::class)
    @Test
    fun add_shouldEmit() = runBlocking {
        // Arrange
        val waveFormsAccumulator = WaveFormsAccumulator(2)
        val growingAccumulator = GrowingAccumulator()
        // Act
        var launched = AtomicBoolean(false)
        val job = CoroutineScope(Dispatchers.Default).async {
            val sequences = mutableListOf<FloatArray>()
            waveFormsAccumulator.flow
                .onSubscription { launched.set(true) }
                .timeout(300.toDuration(DurationUnit.MILLISECONDS)).catch { emit(FloatArray(0)) }
                .takeWhile { it.isNotEmpty() }
                .onEach {
                    println(it.joinTo(StringBuilder(), prefix = "fixed: [", postfix = "]").toString())
                }
                .map {
                    growingAccumulator.growAccumulator(it)
                }
                .onEach {
                    println(it.joinTo(StringBuilder(), prefix = "growing: [", postfix = "]").toString())
                }
                .collect {
                    sequences.add(it)
                }
            sequences
        }
        while (!launched.get()) delay(5)
        waveFormsAccumulator.add(floatArrayOf(.1f, .2f, .3f))
        delay(10)
        waveFormsAccumulator.add(floatArrayOf(.4f, .5f, .6f))
        delay(10)
        waveFormsAccumulator.add(floatArrayOf(.7f, .8f, .9f))
        delay(100)
        while (waveFormsAccumulator.flush()){
            println("flushing...")
            delay(5)
        }
        println("flushed")
        waveFormsAccumulator.reset()
        growingAccumulator.reset()
        waveFormsAccumulator.add(floatArrayOf(.91f, .92f, .93f))
        while (waveFormsAccumulator.flush()) delay(5)
        val sequences = job.await()
        // Assert
        assertEquals(5, sequences.size)
        assertArrayEquals(floatArrayOf(.1f, .2f), sequences[0])
        assertArrayEquals(floatArrayOf(.1f, .2f, .3f, .4f), sequences[1])
        assertArrayEquals(floatArrayOf(.1f, .2f, .3f, .4f, .5f, .6f), sequences[2])
        assertArrayEquals(floatArrayOf(.1f, .2f, .3f, .4f, .5f, .6f, .7f, .8f), sequences[3])
        assertArrayEquals(floatArrayOf(.91f, .92f), sequences[4])
    }
}