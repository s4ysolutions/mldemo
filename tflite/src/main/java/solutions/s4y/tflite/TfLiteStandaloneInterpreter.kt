package solutions.s4y.tflite

import org.tensorflow.lite.DataType
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import solutions.s4y.tflite.base.TfLiteInterpreter
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

class TfLiteStandaloneInterpreter(
    private val interpreter: InterpreterApi,
    inferenceContext: CoroutineContext,
    onClose: () -> Unit
) : TfLiteInterpreter(inferenceContext, inputs(interpreter), output(interpreter), onClose) {

    override fun runInference(input: FloatArray) {
        val buffer = inputBuffers[0].asFloatBuffer()
        val size = min(buffer.capacity(), input.size)
        for (i in 0..<size) {
            buffer.put(input[i])
        }
        for (i in size until buffer.capacity()) {
            buffer.put(0f)
        }
        interpreter.run(inputBuffers[0], outputBuffer)
    }

    companion object {
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