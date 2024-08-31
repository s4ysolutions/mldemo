package solutions.s4y.mldemo.asr.service.whisper

import android.util.Log
import solutions.s4y.tflite.base.TfLiteInterpreter

class EncoderDecoder(private val interpreter: TfLiteInterpreter) {
    enum class Models {
        HuggingfaceTinyAr,
        HuggingfaceTinyEn,
        HuggingfaceBaseAr,
        HuggingfaceBaseEn,
        SergenesEn,
        Sergenes
    }

    // must be run in the same thread as runInference addDelegate
    /**
     * Run inference on the given mel spectrogram
     * @param logMelSpectrogram Flattened [1, 80, 3000] FloatArray of 1 batch x 80 mel bins x 3000 frames
     * @return The list of tokens
     */
    suspend fun transcribe(logMelSpectrogram: FloatArray): IntArray {
        interpreter.run(logMelSpectrogram)
        Log.d(TAG, "Run inference (decode) done in ${interpreter.lastInferenceDuration} ms")
        return interpreter.intOutput
    }

    companion object {
        private const val TAG = "WhisperInferrer"
    }
}