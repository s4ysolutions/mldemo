package solutions.s4y.tflite

import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import solutions.s4y.tflite.base.TfLiteInterpreter
import java.nio.BufferOverflowException
import kotlin.coroutines.CoroutineContext

class TfLiteStandaloneInterpreter(
    private val interpreter: InterpreterApi,
    inferenceContext: CoroutineContext,
    onClose: () -> Unit
) : TfLiteInterpreter(inferenceContext, inputs(interpreter), output(interpreter), onClose) {
    override val lastInferenceDuration: Int
        get() =
            (interpreter.lastNativeInferenceDurationNanoseconds / 1000000).toInt()


    override fun runInference(input: FloatArray) {
        try {
            interpreter.run(inputBuffers[0], outputBuffer)
        } catch (e: BufferOverflowException) {
            Log.e(
                TAG,
                "runInference: BufferOverflowException input: ${inputBuffers[0].capacity()}, output: ${outputBuffer.capacity()}",
                e
            )
        }
    }

    override fun close() {
        interpreter.close()
        super.close()
    }

    companion object {
        const val TAG = "TfLiteStandaloneInterpreter"
        private fun inputs(interpreter: InterpreterApi): List<Pair<IntArray, DataType>> {
            return (0 until interpreter.inputTensorCount).map { i ->
                val shape = interpreter.getInputTensor(i).shape()
                val dataType = interpreter.getInputTensor(i).dataType()
                shape to dataType
            }
        }

        private fun output(interpreter: InterpreterApi): Pair<IntArray, DataType> {
            val shape = interpreter.getOutputTensor(0).shape()
            val dataType = interpreter.getOutputTensor(0).dataType()
            return shape to dataType
        }
    }
}