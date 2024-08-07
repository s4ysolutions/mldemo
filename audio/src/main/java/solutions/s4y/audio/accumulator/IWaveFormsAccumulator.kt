package solutions.s4y.audio.accumulator

import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface IWaveFormsAccumulator<T: Any, A: Any>: Closeable {
    /**
     * Flow of accumulate waveforms of size [batch]
     */
    var batch: Int
    val flow: Flow<FloatArray>
    fun add(pcm: ShortArray)
    fun add(waveForms: FloatArray)
    fun reset()
}