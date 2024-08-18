package solutions.s4y.mldemo.asr.service.whisper

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.mldemo.asr.service.accumulator.GrowingAccumulator
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class WhisperPipeline(
    waveFormsFlow: Flow<FloatArray>,
    private val melSpectrogram: IMelSpectrogram,
    private val model: WhisperInferrer,
    private val tokenizer: WhisperTokenizer,
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
                if (isDecoding.get()) {
                    Log.d(TAG, "Queuing waveforms while decoding is in progress")
                    queuedWaveForms.set(it)
                }
            }
            .filter {
                // if a decoding is in progress, don't start another one
                // let it to complete and skip the new incoming
                // In the other case the recognition can be never completed
                Log.d(TAG, "isDecoding=${isDecoding.get()}")
                !isDecoding.get()
            }
            .flatMapConcat { waveForms ->
                if (isDecoding.compareAndSet(false, true)) {
                    Log.d(TAG, "isDecoding set to true")
                    flow {
                        try {
                            val decoded = decodeWaveForms(waveForms)
                            Log.d(TAG, "emit decoded: $decoded")
                            emit(decoded)
                            // handle the queued
                            while (true) {
                                val queued = queuedWaveForms.getAndSet(null) ?: break
                                Log.d(TAG, "Decoding the queued waveforms")
                                val decodedQ =  decodeWaveForms(queued)
                                Log.d(TAG, "emit decoded (from the queue): $decodedQ")
                                emit(decodedQ)
                            }
                        } finally {
                            Log.d(TAG, "Decoding is not in progress")
                            isDecoding.set(false)
                        }
                    }
                } else {
                    Log.w(TAG, "Decoding is in progress, skip the waveforms (should never happen)")
                    flow { emit("") }
                }
            }
            .filter { it.isNotEmpty() }
    }

    // TODO: should have a variant with list of words
    // internal for testing
    internal suspend fun decodeWaveForms(waveForms: FloatArray): String {
        Log.d(TAG, "decodeWaveForms: getMelSpectrogram start  waveForms.size=${waveForms.size}/${waveForms.size/16000} sec ...")
        var ts = System.currentTimeMillis()
        val mel = melSpectrogram.getMelSpectrogram(waveForms).await()
        Log.d(TAG, "decodeWaveForms: getMelSpectrogram done in ${System.currentTimeMillis() - ts} ms")
        // extract tokens (array of int) from the mel
        ts = System.currentTimeMillis()
        Log.d(TAG, "decodeWaveForms: runInference start ...")
        val tokens = model.runInference(mel).await()
        Log.d(TAG, "decodeWaveForms: runInference done in ${System.currentTimeMillis() - ts} ms")
        // convert tokens to string
        ts = System.currentTimeMillis()
        Log.d(TAG, "decodeWaveForms: decode start ...")
        return tokenizer.decode(tokens).also{
            Log.d(TAG, "decodeWaveForms: decode done in ${System.currentTimeMillis() - ts} ms")
        }
    }

    // TODO: lock/synchronize
    suspend fun reset() {
        growingAccumulator.reset()
        queuedWaveForms.set(null)
        isDecoding.set(false)
    }

    companion object {
        private const val TAG = "WhisperPipeline"
    }
}