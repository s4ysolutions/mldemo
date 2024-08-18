package solutions.s4y.mldemo.asr.service.whisper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import java.io.File
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.coroutines.CoroutineContext

class WhisperInferrer(
    byteBuffer: ByteBuffer, private val inferenceContext: CoroutineContext
) {
    constructor(file: File, inferenceContext: CoroutineContext) : this(
        loadModelFile(file),
        inferenceContext
    )

    constructor(
        context: Context,
        assetModelPath: String,
        inferenceContext: CoroutineContext
    ) : this(
        loadModelAssetFile(
            context,
            assetModelPath
        ),
        inferenceContext
    )

    private var inferrerThreadId: Long = -1
    private lateinit var interpreter: InterpreterApi

    init {
        Log.d(TAG, "Init TensorFlowLite")
        TensorFlowLite.init()

        interpreter = runBlocking(inferenceContext) {
            inferrerThreadId = Thread.currentThread().id
            Log.d(TAG, "Create in thread id: $inferrerThreadId")
            val options = InterpreterApi.Options()
            val compatList = CompatibilityList()
            if (compatList.isDelegateSupportedOnThisDevice) {
                Log.d(TAG, "GPU delegate is supported")
                val delegateOptions =
                    compatList.bestOptionsForThisDevice
                // must be run in the same thread as runInference
                val gpuDelegate = GpuDelegate(delegateOptions)
                options.addDelegate(gpuDelegate)
            } else {
                Log.d(TAG, "GPU delegate is not supported")
            }
            options.setNumThreads(Runtime.getRuntime().availableProcessors())
            var tryInterpreter: InterpreterApi
            try {
                Log.d(TAG, "Create interpreter...")
                tryInterpreter = InterpreterApi.create(byteBuffer, options)
            } catch (e: IllegalArgumentException) {
                Log.w(
                    TAG,
                    "Failed to create interpreter with GPU delegate, falling back to CPU..."
                )
                val optionsFallback = InterpreterApi.Options()
                optionsFallback.setNumThreads(Runtime.getRuntime().availableProcessors())
                tryInterpreter = InterpreterApi.create(byteBuffer, optionsFallback)
            }
            Log.d(TAG, "Interpreter created")
            tryInterpreter
        }

    }

    // must be run in the same thread as runInference addDelegate
    /**
     * Run inference on the given mel spectrogram
     * @param inputData Flattened [1, 80, 3000] FloatArray of 1 batch x 80 mel bins x 3000 frames
     * @return The list of tokens
     */
    suspend fun runInference(inputData: FloatArray): Deferred<IntArray> =
        withContext(inferenceContext) {
            async {
                assert(Thread.currentThread().id == inferrerThreadId) {
                    "runInference must be run in the same thread as the delegate was added"
                }
                Log.d(TAG, "Run inference in thread id: ${Thread.currentThread().id}")
                // Create input tensor
                val inputTensor = interpreter.getInputTensor(0)
                val inputBuffer =
                    TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())

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
                val outputBuffer =
                    TensorBuffer.createFixedSize(outputTensor.shape(), DataType.FLOAT32)

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
                result
            }
        }

    companion object {
        private const val TAG = "WhisperInferrer"

        private fun loadModelFile(file: File): ByteBuffer {
            val inputStream = FileInputStream(file)
            val fileChannel = inputStream.channel
            val startOffset = 0L
            val declaredLength = fileChannel.size()
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                .apply {
                    order(ByteOrder.nativeOrder())
                }
        }

        private fun loadModelAssetFile(context: Context, assetModelPath: String): ByteBuffer {
            val assetFileDescriptor = context.assets.openFd(assetModelPath)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                .apply {
                    order(ByteOrder.nativeOrder())
                }
        }
    }
}