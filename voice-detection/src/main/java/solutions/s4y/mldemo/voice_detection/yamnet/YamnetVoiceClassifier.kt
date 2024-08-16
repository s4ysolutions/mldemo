package solutions.s4y.mldemo.voice_detection.yamnet

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import solutions.s4y.audio.accumulator.PCMFeed
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Classifies voice using Yamnet model
 * @param context Android context used to read model and labels from asset
 * @param filter Optional filter to apply to the sorted array of probabilities indices
 *               which are directly mapped to labels(assets/yamnet_label_list.txt).
 *               If the filter returns false the classification result with string label is not
 *               emitted. This is small optimization
 *               to avoid unnecessary emissions.
 */
class YamnetVoiceClassifier(
    context: Context,
    pcmFeed: PCMFeed,
    private val scopeInference: CoroutineScope = CoroutineScope(Dispatchers.Default)
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

    private val interpreter: Interpreter
    private val labels: List<String>

    private val decoder: IDecoder

    private val outputBuffer0: TensorBuffer
    private var inputTensor: TensorAudio? = null
    private var inputBuffer: TensorBuffer? = null
    private var inputBuf: ByteBuffer? = null
    private var prevWaveFormsSize: Int = 0

    init {
        val assetFileDescriptor = context.assets.openFd(MODEL_PATH)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        labels = context.assets.open(LABELS_PATH).bufferedReader().use { it.readLines() }
        // it works well to recognize speach
        decoder = DecoderLast(labels)
        // for noises like rain, wind, etc
        // decoder = DecoderAverage(labels, 3)

        if (pcmFeed.batch != PCM_BATCH)
            throw Exception("pcmFeed.batch must be 15600, see https://www.kaggle.com/models/google/yamnet/tfLite")
        // pcmFeed.batch = 15600 // https://www.kaggle.com/models/google/yamnet/tfLite
        interpreter = Interpreter(modelFile)
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

    // TODO: disable parallel processing
    private fun waveForms2Probabilities(waveForms: FloatArray): FloatArray {
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
        return probabilities
    }

    private var inferenceJob: Job? = null

    // TODO: add synchronization of inferenceJob modification
    @OptIn(ExperimentalCoroutinesApi::class)
    override val flow = pcmFeed.flow
        .flatMapMerge { waveForms ->
            if (inferenceJob != null) {
                Log.w(TAG, "inferenceJob != null, it will be canceled")
            }
            inferenceJob?.cancel()
            inferenceJob = null
            val deferred = scopeInference.async {
                val probabilities = waveForms2Probabilities(waveForms)
                decoder.add(probabilities)
                IVoiceClassifier.Classes(decoder.classesDescended, waveForms)
            }
            inferenceJob = deferred
            flow {
                val probabilities = deferred.await()
                inferenceJob = null
                emit(probabilities)
            }
        }

    override fun close() {
        interpreter.close()
    }

    override fun labels(classes: List<Int>): List<String> = decoder.labels(classes)

    override fun probabilities(classes: List<Int>): List<Float> = decoder.probabilities(classes)
}