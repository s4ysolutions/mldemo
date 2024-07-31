package solutions.s4y.mldemo.voice_detection.yamnet

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import solutions.s4y.pcm.IPCMFeed
import solutions.s4y.pcm.PCMFeed
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * Classifies voice using Yamnet model
 * @param context Android context used to read model from asset
 * @param scope Coroutine scope to collect upstream flows on and run ML model
 * @param filter Optional filter to apply to the sorted array of probabilities indices
 *               which are directly mapped to labels(assets/yamnet_label_list.txt).
 *               If the filter returns false the classification result with string label is not
 *               emitted. This is small optimization
 *               to avoid unnecessary emissions.
 */
class YamnetClassifier(
    context: Context,
    scope: CoroutineScope,
    filter: ((List<Int>) -> Boolean)? = null
) : IVoiceClassificator {
    companion object {
        private const val MODEL_PATH = "yamnet.tflite"
        private const val LABELS_PATH = "yamnet_label_list.txt"
    }

    private val _classifierFlow: MutableSharedFlow<Pair<List<String>, FloatArray>>
    private val interpreter: Interpreter
    private val job: Job
    private val labels: List<String>
    private val decoder: IDecoder
    private val pcmFeed: IPCMFeed = PCMFeed()
    private val format = TensorAudio.TensorAudioFormat.builder()
        .setChannels(1)
        .setSampleRate(16000)
        .build()


    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            throw UnsupportedOperationException("YamnetClassifier requires Android N or higher")

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
        // decoder = DecoderAverage(labels, 5)

        _classifierFlow = MutableSharedFlow(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        pcmFeed.batch = 15600 // https://www.kaggle.com/models/google/yamnet/tfLite
        interpreter = Interpreter(modelFile)
        val dim = interpreter.getOutputTensor(0).shape()[1]
        // TODO: handle error
        assert(labels.size == dim)

        job = scope.launch {
            pcmFeed.waveForms
                .onEach { waveForms ->
                    val inputTensor = TensorAudio.create(format, waveForms.size)
                    inputTensor.load(waveForms)
                    val inputBuffer = inputTensor.tensorBuffer
                    /*
                    val inputTensor = interpreter.getInputTensor(0)
                    val inputBuffer =
                        TensorBuffer.createFixedSize(inputTensor.shape(), inputTensor.dataType())

                    // Load input data
                    val inputSize =
                        inputTensor.shape()[0] * java.lang.Float.BYTES
                    inputTensor.shape()[0] * inputTensor.shape()[1] * inputTensor.shape()[2] * java.lang.Float.BYTES
                     */
                    // Load input data
                    val inputSize =
                        waveForms.size * java.lang.Float.BYTES
                    val inputBuf = ByteBuffer.allocateDirect(inputSize)
                    inputBuf.order(ByteOrder.nativeOrder())
                    for (input in waveForms) {
                        inputBuf.putFloat(input)
                    }
                    inputBuffer.loadBuffer(inputBuf)

                    val outputTensor0 = interpreter.getOutputTensor(0)
                    val outputBuffer0 =
                        TensorBuffer.createFixedSize(
                            outputTensor0.shape(),
                            outputTensor0.dataType()
                        )

                    interpreter.run(inputBuffer.buffer, outputBuffer0.buffer)
                    val probabilities = outputBuffer0.floatArray
                    decoder.add(probabilities)
                    if (filter == null || filter(decoder.probabilitiesIndicesDescended)) {
                        _classifierFlow.tryEmit(Pair(decoder.labelsDescended, waveForms))
                    }
                }.collect()
        }
    }

    override fun addSamples(samples: ShortArray) {
        pcmFeed.addSamples(samples)
    }

    override val classifierFlow = _classifierFlow

    override fun close() {
        interpreter.close()
        job.cancel()
    }
}