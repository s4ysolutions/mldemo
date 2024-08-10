package solutions.s4y.mldemo.asr.service.whisper

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import solutions.s4y.audio.mel.IMelSpectrogram
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min

class WhisperTFLogMel(context: Context, assetModelPath: String) : IMelSpectrogram {
    private val interpreter: Interpreter
    private val inputBuffer1: TensorBuffer
    private val outputBuffer0: TensorBuffer

    init {
        val assetFileDescriptor = context.assets.openFd(assetModelPath)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(modelFile)
        val inputTensor1 = interpreter.getInputTensor(0)
        inputBuffer1 =
            TensorBuffer.createFixedSize(
                inputTensor1.shape(),
                inputTensor1.dataType()
            )
        val outputTensor0 = interpreter.getOutputTensor(0)
        outputBuffer0 =
            TensorBuffer.createFixedSize(
                outputTensor0.shape(),
                outputTensor0.dataType()
            )
    }

    override fun getMelSpectrogram(samples: FloatArray): FloatArray {
        val inputSize = inputBuffer1.shape[0]
        val inputBuf = ByteBuffer.allocateDirect(inputSize*4)
        inputBuf.order(ByteOrder.nativeOrder())
        for (i in 0..<min(samples.size, inputSize)) {
            inputBuf.putFloat(samples[i])
        }
        inputBuffer1.loadBuffer(inputBuf)
        interpreter.run(inputBuffer1.buffer, outputBuffer0.buffer)
        val logmels = outputBuffer0.floatArray
        return logmels
    }

    companion object {
        private const val MODEL_PATH = "features-extractor.tflite"
    }
}