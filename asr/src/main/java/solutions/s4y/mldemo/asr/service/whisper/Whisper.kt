package solutions.s4y.mldemo.asr.service.whisper

import org.tensorflow.lite.DataType
import java.io.File
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Whisper(
    file: File,
) {
    private val interpreter: InterpreterApi

    init {
        TensorFlowLite.init()

        val options = InterpreterApi.Options()
        val compatList = CompatibilityList()
        if (compatList.isDelegateSupportedOnThisDevice) {
            val delegateOptions = compatList.bestOptionsForThisDevice
            // must be run in the same thread as runInference
            val gpuDelegate = GpuDelegate(delegateOptions)
            options.addDelegate(gpuDelegate)
        } else {
            options.setNumThreads(Runtime.getRuntime().availableProcessors())
        }
        interpreter = InterpreterApi.create(file, options)
    }

    // must be run in the same thread as runInference addDelegate
    /**
     * Run inference on the given mel spectrogram
     * @param inputData Flattened [1, 80, 3000] FloatArray of 1 batch x 80 mel bins x 3000 frames
     * @return The list of tokens
     */
    fun runInference(inputData: FloatArray): IntArray{

        // Create input tensor
        val inputTensor = interpreter.getInputTensor(0)
        val inputBuffer = TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())

        // Load input data
        val inputSize =
            inputTensor.shape()[0] * inputTensor.shape()[1] * inputTensor.shape()[2] * 4 //java.lang.Float.BYTES
        val inputBuf = ByteBuffer.allocateDirect(inputSize)
        inputBuf.order(ByteOrder.nativeOrder())
        for (input in inputData) {
            inputBuf.putFloat(input)
        }
        inputBuffer.loadBuffer(inputBuf)

        // Create output tensor
        val outputTensor = interpreter.getOutputTensor(0)
        val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32)

        // Run inference
        interpreter.run(inputBuffer.buffer, outputBuffer.buffer)

        // Retrieve the results
        val outputLen = outputBuffer.intArray.size
        val result = IntArray(outputLen)
        // val time = System.currentTimeMillis()
        for (i in 0 until outputLen) {
            val token = outputBuffer.buffer.getInt()
            result[i] = token
        }
        // should be monitored
        // (System.currentTimeMillis() - time).toString())
        return result
    }
}