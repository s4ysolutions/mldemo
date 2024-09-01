package solutions.s4y.mldemo.asr.service.whisper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import solutions.s4y.tflite.base.TfLiteInterpreter
import java.io.Closeable

class EncoderDecoder(private val interpreter: TfLiteInterpreter) : Closeable by interpreter {

    val duration: Int get() = interpreter.lastInferenceDuration

    suspend fun transcribe(logMelSpectrogram: FloatArray): IntArray {
        interpreter.run(logMelSpectrogram)
        Log.d(TAG, "Run inference (encode-decode) done in ${interpreter.lastInferenceDuration} ms")
        return interpreter.intOutput
    }

    companion object {
        private const val TAG = "WhisperInferrer"
    }
}