package solutions.s4y.mldemo.voice_detection.yamnet

import android.content.Context
import solutions.s4y.tflite.base.TfLiteFactory
import solutions.s4y.tflite.base.TfLiteInterpreter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YamnetVoiceClassifier @Inject constructor(
    private val tfLiteFactory: TfLiteFactory,
) : IVoiceClassifier {
    private lateinit var decoder: IDecoder
    private lateinit var interpreter: TfLiteInterpreter


    override val inputSize: Int = PCM_BATCH

    override suspend fun initialize(context: Context) {
        interpreter=tfLiteFactory.createInterpreterFromAsset(context, MODEL_PATH, "yamnet")

        val labels: List<String> = context.assets.open(LABELS_PATH).use { inputStream ->
            inputStream.bufferedReader().use { bufferRead ->
                bufferRead.readLines()
            }
        }
        decoder = DecoderLast(labels)
    }

    override suspend fun classify(waveForms: FloatArray): IVoiceClassifier.Classes {
        if (waveForms.size != PCM_BATCH)
            throw Exception("pcmFeed.batch must be 15600, see https://www.kaggle.com/models/google/yamnet/tfLite")

        interpreter.run(waveForms)
        val probabilitiesRaw = interpreter.floatOutput
        decoder.add(probabilitiesRaw)
        return IVoiceClassifier.Classes(decoder.classesDescended, waveForms)
    }

    override fun close() {
        interpreter.close()
    }

    override fun labels(classes: List<Int>): List<String> = decoder.labels(classes)

    override fun probabilities(classes: List<Int>): List<Float> = decoder.probabilities(classes)

    companion object {
        private const val LABELS_PATH = "yamnet_label_list.txt"
        private const val MODEL_PATH = "yamnet.tflite"
        const val PCM_BATCH = 15600
    }

}