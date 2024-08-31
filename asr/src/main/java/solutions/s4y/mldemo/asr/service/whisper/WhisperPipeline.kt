package solutions.s4y.mldemo.asr.service.whisper

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.mldemo.asr.service.accumulator.GrowingAccumulator
import kotlin.coroutines.coroutineContext

class WhisperPipeline(
    waveFormsFlow: Flow<FloatArray>,
    private val melSpectrogram: IMelSpectrogram,
    private val model: DecoderEncoder,
    private val tokenizer: WhisperTokenizer,
) {
    // each next decoding contains all previous waveforms
    private var growingAccumulator: GrowingAccumulator = GrowingAccumulator()

    // only one waveforms is decoding at a time
    private var mutableIStateDecoding = MutableStateFlow(false)

    // the flow of decoded strings
    val flow: Flow<String>
    val flowIsDecoding: StateFlow<Boolean>
        get() = mutableIStateDecoding

    init {
        flow = waveFormsFlow
            .conflate()
            .map { waveForms ->
                if (waveForms.isEmpty()) {
                    return@map ""
                }
                if (mutableIStateDecoding.compareAndSet(false, true)) {
                    try {
                        Log.d(TAG, "isDecoding set to true")
                        val decoded = decodeWaveForms(waveForms)
                        if (decoded != null) {
                            Log.d(TAG, "emit decoded: $decoded")
                            return@map decoded
                        }
                    } finally {
                        Log.d(TAG, "isDecoding set to false")
                        mutableIStateDecoding.value = false
                    }
                } else {
                    Log.w(
                        TAG,
                        "Decoding is in progress, skip the waveforms (should never happen)"
                    )
                }
                ""
            }
            .filter { it.isNotEmpty() }
    }

    // TODO: should have a variant with list of words
    // internal for testing
    internal suspend fun decodeWaveForms(waveForms: FloatArray): String? {
        // try { let it be handled by the caller
        if (!coroutineContext.isActive) {
            Log.d(TAG, "decodeWaveForms: coroutine is not active")
            return null
        }
        val mel = melSpectrogram.getMelSpectrogram(waveForms) ?: return null
        if (!coroutineContext.isActive) {
            Log.d(TAG, "decodeWaveForms: coroutine is not active")
            return null
        }
        val tokens = model.runInference(mel) ?: return null
        if (!coroutineContext.isActive) {
            Log.d(TAG, "decodeWaveForms: coroutine is not active")
            return null
        }
        return tokenizer.decode(tokens).also {
            Log.d(TAG, "decodeWaveForms: decoded: $it")
        }
        /*
    }catch (e: ChildCancellationException) {
        Log.e(TAG, "decodeWaveForms: ${e.message}")
        return null
    }
         */
    }

    fun reset() = runBlocking {
        growingAccumulator.reset()
        mutableIStateDecoding.value = false
    }

    companion object {
        private const val TAG = "WhisperPipeline"
    }
}