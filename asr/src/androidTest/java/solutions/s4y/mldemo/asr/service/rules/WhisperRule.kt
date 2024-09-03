package solutions.s4y.mldemo.asr.service.rules

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.gson.JsonParser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import solutions.s4y.mldemo.googleServicesOptionsBuilder
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.audio.pcm.PCMAssetWavProvider
import solutions.s4y.mldemo.asr.service.AsrModels
import solutions.s4y.mldemo.asr.service.gcs.gcsEncoderDecoderPath
import solutions.s4y.mldemo.asr.service.gcs.gcsFeaturesExtractorPath
import solutions.s4y.mldemo.asr.service.whisper.EncoderDecoder
import solutions.s4y.mldemo.asr.service.logmel.TFLiteLogMel
import solutions.s4y.mldemo.asr.service.whisper.WhisperTokenizer
import solutions.s4y.tflite.TfLiteStandaloneFactory
import solutions.s4y.tflite.base.TfLiteFactory
import java.io.InputStreamReader

class WhisperRule : MethodRule {
    companion object {
        private var initialized = false
        private val sync = Any()
    }

    private fun readTokensFromJsonAsset(name: String): IntArray {
        val inputStream = context.assets.open(name)
        val reader = InputStreamReader(inputStream)
        val tokensData: MutableList<Int> = mutableListOf()
        val array = JsonParser.parseReader(reader).asJsonArray
        for (i in 0 until array.size()) {
            val token = array[i].asInt
            tokensData.add(token)
        }
        reader.close()
        return tokensData.toIntArray()
    }

    private fun readMelFromJsonAsset(name: String): IntArray {
        val inputStream = context.assets.open(name)
        val reader = InputStreamReader(inputStream)
        val tokensData: MutableList<MutableList<Int>> = mutableListOf()
        val array = JsonParser.parseReader(reader).asJsonArray
        for (i in 0 until array.size()) {
            val batch = array[i].asJsonArray
            val batchL = mutableListOf<Int>()
            tokensData.add(batchL)
            for (j in 0 until batch.size()) {
                val token = batch[j].asInt
                batchL.add(token)
            }
        }
        reader.close()
        return tokensData.flatten().toIntArray()
    }

    private fun readFloatArrayFromJsonAsset(name: String): FloatArray {
        val inputStream = context.assets.open(name)
        val reader = InputStreamReader(inputStream)
        val melData: MutableList<MutableList<MutableList<Float>>> = mutableListOf()
        val array = JsonParser.parseReader(reader).asJsonArray
        for (i in 0 until array.size()) {
            val batch = array[i].asJsonArray
            val batchL = mutableListOf<MutableList<Float>>()
            melData.add(batchL)
            for (j in 0 until batch.size()) {
                val sample = batch[j].asJsonArray
                val sampleL = mutableListOf<Float>()
                batchL.add(sampleL)
                for (k in 0 until sample.size()) {
                    val bin = sample[k].asFloat
                    sampleL.add(bin)
                }
            }
        }
        reader.close()
        return melData.flatten().flatten().toFloatArray()
    }

    private lateinit var context: Context

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val inferenceContext = newSingleThreadContext("YamnetVoiceClassifierTest")

    /*
        val waveFormsAccumulator: WaveFormsAccumulatorFlow by lazy {
            WaveFormsAccumulatorFlow()
        }

        val pcmFeed: PCMFeed by lazy {
            PCMFeed()
        }
    */
    private val tfLiteFactory: TfLiteFactory by lazy {
        TfLiteStandaloneFactory()
    }

    val assetTFLiteLogMel: IMelSpectrogram by lazy {
        runBlocking {
            TFLiteLogMel(
                tfLiteFactory.createInterpreterFromGCS(
                    context,
                    gcsFeaturesExtractorPath(),
                    "test-logmel"
                )
            )
        }
    }

    val gcsHuggingfaceTinyEn: EncoderDecoder by lazy {
        runBlocking {
            EncoderDecoder(
                tfLiteFactory.createInterpreterFromGCS(
                    context, gcsEncoderDecoderPath(
                        AsrModels.HuggingfaceTinyEn,
                    ),
                    "test-huggingface-tiny-en"
                )
            )
        }
    }

    val gcsHuggingfaceTinyAr: EncoderDecoder by lazy {
        runBlocking {
            EncoderDecoder(
                tfLiteFactory.createInterpreterFromGCS(
                    context, gcsEncoderDecoderPath(
                        AsrModels.HuggingfaceTinyAr,
                    ),
                    "test-huggingface-tiny-ar"
                )
            )
        }
    }

    val gcsHuggingfaceBaseEn: EncoderDecoder by lazy {
        runBlocking {
            EncoderDecoder(
                tfLiteFactory.createInterpreterFromGCS(
                    context, gcsEncoderDecoderPath(
                        AsrModels.HuggingfaceBaseEn,
                    ),
                    "test-huggingface-base-en"
                )
            )
        }
    }

    val gcsHuggingfaceBaseAr: EncoderDecoder by lazy {
        runBlocking {
            EncoderDecoder(
                tfLiteFactory.createInterpreterFromGCS(
                    context, gcsEncoderDecoderPath(
                        AsrModels.HuggingfaceBaseAr,
                    ),
                    "test-huggingface-base-ar"
                )
            )
        }
    }

    val gcsSergenesTiny: EncoderDecoder by lazy {
        runBlocking {
            EncoderDecoder(
                tfLiteFactory.createInterpreterFromGCS(
                    context, gcsEncoderDecoderPath(
                        AsrModels.Sergenes,
                    ),
                    "test-sergenes-tiny"
                )
            )
        }
    }

    val gcsSergenesTinyEn: EncoderDecoder by lazy {
        runBlocking {
            EncoderDecoder(
                tfLiteFactory.createInterpreterFromGCS(
                    context, gcsEncoderDecoderPath(
                        AsrModels.SergenesEn,
                    ),
                    "test-sergenes-tiny-en"
                )
            )
        }
    }

    val tokenizerHuggingface: WhisperTokenizer by lazy {
        runBlocking {
            WhisperTokenizer.loadFromGCS(context)
        }
    }

    val testPCMAr11: ShortArray by lazy {
        val p = PCMAssetWavProvider(context, "adam/1-1.wav")
        p.shorts
    }

    val testWaveFormsAr11: FloatArray by lazy {
        val p = PCMAssetWavProvider(context, "adam/1-1.wav")
        p.floats
    }

    val testWaveFormsEn: FloatArray by lazy {
        val p = PCMAssetWavProvider(context, "OSR_us_000_0030_16k.wav")
        p.floats
    }

    val testMelAr11: FloatArray by lazy {
        readFloatArrayFromJsonAsset("adam/1-1-mel.json")
    }

    val testMelEn: FloatArray by lazy {
        readFloatArrayFromJsonAsset("OSR_us_000_0030_16k-mel.json")
    }

    val testTokensAr11: IntArray by lazy {
        readTokensFromJsonAsset("adam/1-1-tokens.json")
    }

    val testTokensEn: IntArray by lazy {
        readTokensFromJsonAsset("OSR_us_000_0030_16k-tokens.json")
    }

    val tsetTokesSet: Set<Int> by lazy {
        testTokensAr11.toSet()
    }

    val testTranscriptionAr11: String by lazy {
        val inputStream = context.assets.open("adam/1-1-transcription.txt")
        val reader = InputStreamReader(inputStream)
        val transcription = reader.readText()
        reader.close()
        transcription
    }

    val testTranscriptionEnTinyTimestamp: String by lazy {
        "<|0.00|> Paint the sockets in the wall, dull green. The child crawled into the dense grass,<|9.00|><|9.00|> Bride's failware honest men work. Trampal the spark, else the flames will spread.<|16.00|><|16.00|> The hilt of the sword was carved with fine designs. A round hole was drilled through the thin board.<|25.00|><|25.00|>"
    }

    val testTranscriptionEn: String by lazy {
        " Paint the sockets in the wall dull green. The child crawled into the dense grass. Bribes fail where honest men work. Trample the spark, else the flames will spread. The hilt of the sword was carved with fine designs. A round hole was drilled through the thin board. Footprints showed the path he took up the beach."
    }

    val testTranscriptionAr11WithError: String by lazy {
        val inputStream = context.assets.open("adam/1-1-transcription-with-error.txt")
        val reader = InputStreamReader(inputStream)
        val transcription = reader.readText()
        reader.close()
        transcription
    }

    override fun apply(base: Statement, method: FrameworkMethod?, target: Any?): Statement {
        val app = ApplicationProvider.getApplicationContext<Application>()
        synchronized(sync) {
            if (!initialized) {
                val builder: FirebaseOptions.Builder = googleServicesOptionsBuilder()
                val options = builder.build()
                FirebaseApp.initializeApp(app, options, "[DEFAULT]")
                initialized = true
            }
        }
        context = app

        return object : Statement() {
            override fun evaluate() {
                base.evaluate()
            }
        }
    }
}