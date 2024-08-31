package solutions.s4y.tflite

import android.content.Context
import android.util.Log
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.InterpreterApi.Options
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory
import solutions.s4y.tflite.base.TfLiteFactory
import solutions.s4y.tflite.base.TfLiteInterpreter
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

class TfLiteStandaloneFactory(): TfLiteFactory() {
    override suspend fun initialize(context: Context) {
        Log.d(TAG, "initialize TensorFlowLite")
        TensorFlowLite.init()
    }

    override suspend fun createInterpreter(
        context: Context,
        inferenceContext: CoroutineContext,
        modelBuffer: ByteBuffer,
        onClose: () -> Unit
    ): TfLiteInterpreter {
        val options = Options()
        val compatList = CompatibilityList()
        if (compatList.isDelegateSupportedOnThisDevice) {
            Log.d(TAG, "GPU delegate is supported")
            val delegateOptions =
                compatList.bestOptionsForThisDevice
            delegateOptions.setForceBackend(GpuDelegateFactory.Options.GpuBackend.OPENGL)
            // must be run in the same thread as runInference
            val gpuDelegate = GpuDelegate(delegateOptions)
            options.addDelegate(gpuDelegate)
        } else {
            Log.d(TAG, "GPU delegate is not supported")
            options.setNumThreads(Runtime.getRuntime().availableProcessors())
        }
        val interpreter = InterpreterApi.create(modelBuffer, options)
        Log.d(TAG, "Interpreter created")
        return TfLiteStandaloneInterpreter(interpreter, inferenceContext, onClose)
    }

    companion object {
        private const val TAG = "TfLiteStandaloneFactory"
    }
}