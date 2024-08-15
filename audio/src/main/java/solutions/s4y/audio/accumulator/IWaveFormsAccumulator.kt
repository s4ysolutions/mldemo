package solutions.s4y.audio.accumulator

import kotlinx.coroutines.flow.SharedFlow
import java.io.Closeable

interface IWaveFormsAccumulator<T: Any, A: Any>: Closeable {
    /**
     * Flow of accutmulate waveforms of size [batch]
     */
    val batch: Int
    val flow: SharedFlow<FloatArray>
    /**
     * Add waveforms to accumulator and tries to call flush at the end
     * @return
     *  - true if the data were added and ether the batch is not fulfilled or
     * it is successfully consumed by the client
     *  - false in case the attempt to emit batch to the consumers was unsuccessful
     *    and the data stays in the accumulator until the next attempt of adding or
     *    explicit flush.
     */
    suspend fun tryAdd(waveForms: FloatArray): Boolean
    suspend fun add(waveForms: FloatArray)
    /**
     * Forces to emit the accumulated waveforms. Intended to be called by the consumer in case
     * if it's ready to consume the data.
     * @return true if there's something to flush (consumer is not ready to consume all the data)
     * Return false if there's no consumers ready to receive the data or there's nothing to flush
     */
    suspend fun flush(): Boolean
    suspend fun reset()
}

