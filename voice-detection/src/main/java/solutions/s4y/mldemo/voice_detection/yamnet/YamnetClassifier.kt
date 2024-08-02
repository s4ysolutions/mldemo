package solutions.s4y.mldemo.voice_detection.yamnet

import android.content.Context
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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
 * @param context Android context used to read model and labels from asset
 * @param filter Optional filter to apply to the sorted array of probabilities indices
 *               which are directly mapped to labels(assets/yamnet_label_list.txt).
 *               If the filter returns false the classification result with string label is not
 *               emitted. This is small optimization
 *               to avoid unnecessary emissions.
 */
class YamnetClassifier(
    context: Context,
    filter: ((List<Int>) -> Boolean)? = null
) : IVoiceClassificator {
    companion object {
        private const val MODEL_PATH = "yamnet.tflite"
        private const val LABELS_PATH = "yamnet_label_list.txt"
        private val format = TensorAudio.TensorAudioFormat.builder()
            .setChannels(1)
            .setSampleRate(16000)
            .build()
    }

    private val interpreter: Interpreter
    private val labels: List<String>
    private val decoder: IDecoder
    private val pcmFeed: IPCMFeed = PCMFeed()

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
        // decoder = DecoderAverage(labels, 5)

        pcmFeed.batch = 15600 // https://www.kaggle.com/models/google/yamnet/tfLite
        interpreter = Interpreter(modelFile)
        val dim = interpreter.getOutputTensor(0).shape()[1]
        // TODO: handle error
        assert(labels.size == dim)
    }

    private fun waveForms2Probabilities(waveForms: FloatArray): FloatArray {
        val inputTensor = TensorAudio.create(format, waveForms.size)
        inputTensor.load(waveForms)
        val inputBuffer = inputTensor.tensorBuffer
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
        return probabilities
    }

    override fun addSamples(samples: ShortArray) {
        pcmFeed.addSamples(samples)
    }

    override val probabilitiesFlow =  pcmFeed.flow
        .map{ waveForms -> IVoiceClassificator.Probabilities(waveForms2Probabilities(waveForms), waveForms) }
        .filter { p ->
            decoder.add(p.probabilities)
            filter == null || filter(decoder.probabilitiesIndicesDescended)
        }

    override val labelsFlow = probabilitiesFlow
        .map { p ->
            IVoiceClassificator.Labels(decoder.labelsDescended, p.waveForms)
        }

    override fun close() {
        pcmFeed.close()
        interpreter.close()
    }
}