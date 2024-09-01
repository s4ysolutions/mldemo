package solutions.s4y.mldemo.asr.service.logmel

import android.util.Log
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.tflite.base.TfLiteInterpreter
import java.io.Closeable

class TFLiteLogMel(
    private val interpreter: TfLiteInterpreter
) :
    IMelSpectrogram, Closeable by interpreter {

    val duration: Long get() = interpreter.lastInferenceDuration

    override suspend fun getMelSpectrogram(waveForms: FloatArray): FloatArray {
        interpreter.run(waveForms)
        Log.d(
            TAG,
            "Run inference (getMelSpectrogram) done in ${interpreter.lastInferenceDuration} ms"
        )
        return interpreter.floatOutput
    }

    companion object {
        private const val TAG = "TFLiteLogMel"
    }
}