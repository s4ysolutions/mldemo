package solutions.s4y.mldemo.voice_transcription.service.implementation

import org.tensorflow.lite.DataType
import java.io.File
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import solutions.s4y.mldemo.voice_transcription.service.dependencies.IMelVoiceTranscriber
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WhisperVoiceTranscriber(
    file: File,
) : IMelVoiceTranscriber {
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
    private fun runInference(inputData: FloatArray): List<String> {
        // Create input tensor
        val inputTensor = interpreter.getInputTensor(0)
        val inputBuffer = TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())

        // Create output tensor
        val outputTensor = interpreter.getOutputTensor(0)
        val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32)

        // Load input data
        val inputSize =
            inputTensor.shape()[0] * inputTensor.shape()[1] * inputTensor.shape()[2] * 8 //java.lang.Float.BYTES
        val inputBuf = ByteBuffer.allocateDirect(inputSize)
        inputBuf.order(ByteOrder.nativeOrder())
        for (input in inputData) {
            inputBuf.putFloat(input)
        }

        inputBuffer.loadBuffer(inputBuf)

        // Run inference
        interpreter.run(inputBuffer.buffer, outputBuffer.buffer)

        // Retrieve the results
        val outputLen = outputBuffer.intArray.size
        val result = StringBuilder()
        // val time = System.currentTimeMillis()
        for (i in 0 until outputLen) {
            val token = outputBuffer.buffer.getInt()
            print(token)
            /* TODO
            if (token == mWhisperUtil.tokenEOT) break

            // Get word for token and Skip additional token
            if (token < mWhisperUtil.tokenEOT) {
                val word = mWhisperUtil.getWordFromToken(token)
                Log.d(TAG, "Adding token: $token, word: $word")
                result.append(word)
            } else {
                if (token == mWhisperUtil.tokenTranscribe) Log.d(TAG, "It is Transcription...")
                if (token == mWhisperUtil.tokenTranslate) Log.d(TAG, "It is Translation...")
                val word = mWhisperUtil.getWordFromToken(token)
                Log.d(TAG, "Skipping token: $token, word: $word")
            }
             */
        }
        // should be monitored
        // (System.currentTimeMillis() - time).toString())
        return result.toString().split(' ')
    }

    override fun transcribe(melSpectrogram: FloatArray): List<String> =
        runInference(melSpectrogram)
}