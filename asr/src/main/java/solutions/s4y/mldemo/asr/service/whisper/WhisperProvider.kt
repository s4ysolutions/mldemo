package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.mldemo.asr.service.accumulator.GrowingAccumulator
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

class WhisperProvider(
    waveFormsFlow: Flow<FloatArray>,
    private val melSpectrogram: IMelSpectrogram,
    private val model: WhisperInferrer,
    private val tokenizer: WhisperTokenizer,
    private val decodeScope: CoroutineScope,
) {
    // each next decoding contains all previous waveforms
    private var growingAccumulator: GrowingAccumulator = GrowingAccumulator()

    // only one waveforms is decoding at a time
    private var isDecoding = AtomicBoolean(false)

    // waveforms that are queued while decoding is in progress
    // only one, the very last queued waveforms makes sense
    private var queuedWaveForms = AtomicReference<FloatArray?>(null)

    // the flow of decoded strings
    val flow: Flow<String>

    init {
        @Suppress("OPT_IN_USAGE")
        flow = waveFormsFlow
            .onEach {
                // if decoding is in progress, queue the waveforms
                // overwriting the previous queued waveforms
                if (isDecoding.get())
                    queuedWaveForms.set(it)
            }
            .filter {
                // if a decoding is in progress, don't start another one
                // let it to complete and skip the new incoming
                // In the other case the recognition can be never completed
                !isDecoding.get()
            }
            // TODO: flatMapMerge + filter should be replaced with transform
            .flatMapMerge { waveForms ->
                if (isDecoding.compareAndSet(false, true)) {
                    flow {
                        try {
                            val decoded = decodeScope.async { decodeWaveForms(waveForms) }
                            emit(decoded.await())
                            // handle the queued
                            while (true) {
                                val queued = queuedWaveForms.getAndSet(null) ?: break
                                val decodedQ = decodeScope.async { decodeWaveForms(queued) }
                                emit(decodedQ.await())
                            }
                        } finally {
                            isDecoding.set(false)
                        }
                    }
                } else {
                    flow { emit("") }
                }
            }
            .filter { it.isNotEmpty() }
    }

    // TODO: should have a variant with list of words
    // internal for testing
    internal fun decodeWaveForms(waveForms: FloatArray): String {
        // pad or truncate to 30 seconds
        val inb = if (waveForms.size == 16000 * 30)
            waveForms
        else {
            val b = FloatArray(16000 * 30)
            waveForms.copyInto(b, endIndex = min(16000 * 30, waveForms.size))
            b
        }
        // get mel spectrogram from the waveforms
        val mel = melSpectrogram.getMelSpectrogram(inb)
        // extract tokens (array of int) from the mel
        val tokens = model.runInference(mel)
        // convert tokens to string
        return tokenizer.decode(tokens)
    }

    // TODO: lock/synchronize
    suspend fun reset() {
        decodeScope.cancel()
        growingAccumulator.reset()
        queuedWaveForms.set(null)
        isDecoding.set(false)
    }
}