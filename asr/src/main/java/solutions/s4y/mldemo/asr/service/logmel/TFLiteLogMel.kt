package solutions.s4y.mldemo.asr.service.logmel

import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.tflite.base.TfLiteInterpreter

class TFLiteLogMel(
    private val interpreter: TfLiteInterpreter
) :
    IMelSpectrogram {
    override suspend fun getMelSpectrogram(waveForms: FloatArray): FloatArray {
        interpreter.run(waveForms)
        return interpreter.floatOutput
    }
}