package solutions.s4y.mldemo.voice_detection.yamnet

import android.content.Context
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.TensorFlowLite
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.gpu.GpuDelegateFactory
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.coroutines.CoroutineContext

/**
 * Classifies voice using Yamnet model
 * @param context Android context used to read model and labels from asset
 * @param waveFormsFlow flow of PCM float arrays of PCM_BATCH size
 */
class YamnetVoiceClassifier(
    context: Context,
    waveFormsFlow: Flow<FloatArray>,
    private val inferenceContext: CoroutineContext
    //private val scopeInference: CoroutineScope
) : IVoiceClassifier {
    companion object {
        private const val LABELS_PATH = "yamnet_label_list.txt"
        private const val MODEL_PATH = "yamnet.tflite"
        const val PCM_BATCH = 15600
        private val TAG: String = YamnetVoiceClassifier::class.java.simpleName

        private val format = TensorAudio.TensorAudioFormat.builder()
            .setChannels(1)
            .setSampleRate(16000)
            .build()
    }

    private val decoder: IDecoder
    private var inferrerThreadId: Long = -1
    private var inputTensor: TensorAudio? = null
    private var inputBuffer: TensorBuffer? = null
    private var inputBuf: ByteBuffer? = null
    private val interpreter: InterpreterApi
    private val labels: List<String>
    private val outputBuffer0: TensorBuffer
    private var prevWaveFormsSize: Int = 0

    init {
        Log.d(TAG, "Init TensorFlowLite")
        TensorFlowLite.init()

        val assetFileDescriptor = context.assets.openFd(MODEL_PATH)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        labels = context.assets.open(LABELS_PATH).bufferedReader().use { it.readLines() }
        // it works well to recognize speech
        decoder = DecoderLast(labels)
        // for noises like rain, wind, etc
        // decoder = DecoderAverage(labels, 3)

        val options = InterpreterApi.Options()
        val compatList = CompatibilityList()
        interpreter = runBlocking(inferenceContext) {
            inferrerThreadId = Thread.currentThread().id
            Log.d(TAG, "Create in thread id: $inferrerThreadId")
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
            }
            options.setNumThreads(Runtime.getRuntime().availableProcessors())
            var interpreter: InterpreterApi
            try {
                Log.d(TAG, "Create interpreter...")
                interpreter = InterpreterApi.create(modelFile, options)
            } catch (e: IllegalArgumentException) {
                Log.w(
                    TAG,
                    "Failed to create interpreter with GPU delegate, falling back to CPU..."
                )
                val optionsFallback = InterpreterApi.Options()
                optionsFallback.setNumThreads(Runtime.getRuntime().availableProcessors())
                interpreter = InterpreterApi.create(modelFile, optionsFallback)
            }
            Log.d(TAG, "Interpreter created")
            interpreter
        }
        val dim = interpreter.getOutputTensor(0).shape()[1]
        // TODO: handle error
        assert(labels.size == dim)

        val outputTensor0 = interpreter.getOutputTensor(0)
        outputBuffer0 =
            TensorBuffer.createFixedSize(
                outputTensor0.shape(),
                outputTensor0.dataType()
            )
    }

    private suspend fun waveForms2Probabilities(waveForms: FloatArray): FloatArray =
        withContext(inferenceContext) {
            assert(Thread.currentThread().id == inferrerThreadId) {
                "runInference must be run in the same thread as the delegate was added ($inferrerThreadId), but was ${Thread.currentThread()}(${Thread.currentThread().id})"
            }
            var _inputTensor = inputTensor
            var _inputBuffer = inputBuffer
            var _inputBuf = inputBuf
            if (_inputTensor == null || _inputBuffer == null || _inputBuf == null || prevWaveFormsSize != waveForms.size) {
                _inputTensor = TensorAudio.create(format, waveForms.size)
                _inputBuffer = _inputTensor.tensorBuffer
                val inputSize = waveForms.size * java.lang.Float.BYTES
                _inputBuf = ByteBuffer.allocateDirect(inputSize)
                _inputBuf.order(ByteOrder.nativeOrder())
                inputTensor = _inputTensor
                inputBuffer = _inputBuffer
                inputBuf = _inputBuf
                prevWaveFormsSize = waveForms.size
            }
            _inputTensor!!.load(waveForms)
            for (input in waveForms) {
                _inputBuf!!.putFloat(input)
            }
            _inputBuffer!!.loadBuffer(_inputBuf!!)

            interpreter.run(_inputBuffer.buffer, outputBuffer0.buffer)
            val probabilities = outputBuffer0.floatArray
            probabilities
        }

    // TODO: add synchronization of inferenceJob modification
    @OptIn(ExperimentalCoroutinesApi::class)
    override val flow = waveFormsFlow
        // TODO: it should be flatMapLatest
        // but i couldn't make robust test for it
        .flatMapConcat { waveForms ->
            if (waveForms.size != PCM_BATCH)
                throw Exception("pcmFeed.batch must be 15600, see https://www.kaggle.com/models/google/yamnet/tfLite")
            flow {
                val probabilitiesRaw = waveForms2Probabilities(waveForms)
                decoder.add(probabilitiesRaw)
                val probabilities =
                    IVoiceClassifier.Classes(decoder.classesDescended, waveForms)
                emit(probabilities)
            }
        }

    override fun close() {
        interpreter.close()
    }

    override fun labels(classes: List<Int>): List<String> = decoder.labels(classes)

    override fun probabilities(classes: List<Int>): List<Float> = decoder.probabilities(classes)
}