package solutions.s4y.mldemo.asr.service.whisper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.firebase.FirebaseBlob
import solutions.s4y.mldemo.asr.service.gcs.gcsFeaturesExtractorPath
import solutions.s4y.mldemo.asr.service.whisper.extensions.toByteBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

class WhisperTFLogMel(
    byteBuffer: ByteBuffer, private val inferenceContext: CoroutineContext
) :
    IMelSpectrogram {

    private var interpreter: InterpreterApi
    private var inputBuffer1: TensorBuffer
    private var outputBuffer0: TensorBuffer
    private var inferrerThreadId: Long = 0

    init {
        Log.d(TAG, "Init TensorFlowLite")
        TensorFlowLite.init()

        runBlocking(inferenceContext) {
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
            interpreter = tryInterpreter
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
    }


    override suspend fun getMelSpectrogram(waveForms: FloatArray): FloatArray? =
        withContext(inferenceContext) {
            if (!coroutineContext.isActive) return@withContext null
            assert(Thread.currentThread().id == inferrerThreadId) {
                "runInference must be run in the same thread as the delegate was added"
            }
            val inputSize = inputBuffer1.shape[0]
            val inputBuf = ByteBuffer.allocateDirect(inputSize * 4)
            inputBuf.order(ByteOrder.nativeOrder())
            for (i in 0..<min(waveForms.size, inputSize)) {
                inputBuf.putFloat(waveForms[i])
            }
            inputBuffer1.loadBuffer(inputBuf)
            if (!coroutineContext.isActive) return@withContext null
            Log.d(
                TAG,
                "Run inference(getMelSpectrogram) start waveForms.size=${waveForms.size}/${waveForms.size / 16000} sec ..."
            )
            val ts = System.currentTimeMillis()
            interpreter.run(inputBuffer1.buffer, outputBuffer0.buffer)
            Log.d(
                TAG,
                "Run inference(getMelSpectrogram) done in ${System.currentTimeMillis() - ts} ms"
            )
            val logmels = outputBuffer0.floatArray
            logmels
        }

    companion object {
        private const val TAG = "WhisperTFLogMel"

        suspend fun loadFromGCS(
            context: Context,
            coroutineContext: CoroutineContext
        ): WhisperTFLogMel {
            val gcsPath = gcsFeaturesExtractorPath()
            val localFile = File(context.filesDir, gcsPath)
            FirebaseBlob(gcsPath, localFile).get()
            val byteBuffer = localFile.toByteBuffer()
            return WhisperTFLogMel(byteBuffer, coroutineContext)
        }
    }
}