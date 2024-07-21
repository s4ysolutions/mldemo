package solutions.s4y.voice_recognition.tf.model.whisper

import android.content.Context
import android.util.Log
import solutions.s4y.mel.IMelSpectrogramProvider
import solutions.s4y.voice_recognition.IVoiceRecognizer
import java.io.FileInputStream
import java.nio.channels.FileChannel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

// https://github.com/farmaker47/Talk_and_execute/blob/whisper_locally/app/src/main/java/com/example/talkandexecute/whisperengine/WhisperEngine.kt
class WhisperVoiceRecognizer(
    context: Context,
    modelAssetPath: String,
    private val melSpectrogramProvider: IMelSpectrogramProvider
) : IVoiceRecognizer {
    companion object {
        private val TAG = "WhisperVoiceRecognizer"
        private fun loadModel(context: Context, modelPath: String):Interpreter {
            val fileDescriptor = context.assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            fileDescriptor.close()

            val tfliteOptions = Interpreter.Options()
            tfliteOptions.setNumThreads(Runtime.getRuntime().availableProcessors())

            return Interpreter(retFile, tfliteOptions)
        }
    }

    private class WhisperUtil {
        companion object {
            const val WHISPER_SAMPLE_RATE = 16000
            const val WHISPER_CHUNK_SIZE = 30
        }
    }

    private val interpreter: Interpreter

    init {
        interpreter = loadModel(context, modelAssetPath)
    }

    private fun getMelSpectrogram(samples: FloatArray): FloatArray {
        // Get samples in PCM_FLOAT format
        val time = System.currentTimeMillis()
        val fixedInputSize = WhisperUtil.WHISPER_SAMPLE_RATE * WhisperUtil.WHISPER_CHUNK_SIZE
        val inputSamples = FloatArray(fixedInputSize)
        val copyLength = Math.min(samples.size, fixedInputSize)
        System.arraycopy(samples, 0, inputSamples, 0, copyLength)
        val time2 = System.currentTimeMillis()
        val value = melSpectrogramProvider.getMelSpectrogram(inputSamples, fixedInputSize)
        return value
    }

    private fun runInference(inputData: FloatArray): String {
        // Create input tensor
        val inputTensor = interpreter.getInputTensor(0)
        val inputBuffer = TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())
        Log.d(TAG, "Input Tensor Dump ===>")
        printTensorDump(inputTensor)

        // Create output tensor
        val outputTensor = interpreter!!.getOutputTensor(0)
        val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32)
        Log.d(TAG, "Output Tensor Dump ===>")
        printTensorDump(outputTensor)

        // Load input data
        val inputSize =
            inputTensor.shape()[0] * inputTensor.shape()[1] * inputTensor.shape()[2] * java.lang.Float.BYTES
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
        Log.d(TAG, "output_len: $outputLen")
        val result = StringBuilder()
        val time = System.currentTimeMillis()
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
        Log.v("inference_time_decode", (System.currentTimeMillis() - time).toString())
        return result.toString()
    }

    private fun printTensorDump(tensor: Tensor) {
        Log.d(TAG, "  shape.length: " + tensor.shape().size)
        for (i in tensor.shape().indices) Log.d(TAG, "    shape[" + i + "]: " + tensor.shape()[i])
        Log.d(TAG, "  dataType: " + tensor.dataType())
        Log.d(TAG, "  name: " + tensor.name())
        Log.d(TAG, "  numBytes: " + tensor.numBytes())
        Log.d(TAG, "  index: " + tensor.index())
        Log.d(TAG, "  numDimensions: " + tensor.numDimensions())
        Log.d(TAG, "  numElements: " + tensor.numElements())
        Log.d(TAG, "  shapeSignature.length: " + tensor.shapeSignature().size)
        Log.d(TAG, "  quantizationParams.getScale: " + tensor.quantizationParams().scale)
        Log.d(TAG, "  quantizationParams.getZeroPoint: " + tensor.quantizationParams().zeroPoint)
        Log.d(TAG, "==================================================================")
    }

    override fun recognize(pcm: FloatArray): String {
        // Calculate Mel spectrogram
        Log.d(TAG, "Calculating Mel spectrogram...")
        val time = System.currentTimeMillis()
        val melSpectrogram = getMelSpectrogram(pcm)
        Log.d(TAG, "Mel spectrogram is calculated...!")
        Log.v("inference_time_mel", (System.currentTimeMillis() - time).toString())

        // Perform inference
        val time2 = System.currentTimeMillis()
        val result = runInference(melSpectrogram)
        Log.d(TAG, "Inference is executed...!")
        Log.v("inference_time_mel", (System.currentTimeMillis() - time2).toString())
        return result
    }
}