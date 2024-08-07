package solutions.s4y.mldemo.asr.service.rules

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import solutions.s4y.audio.accumulator.PCMFeed
import solutions.s4y.audio.accumulator.WaveFormsAccumulator
import solutions.s4y.firebase.FirebaseBlob
import solutions.s4y.mldemo.googleServicesOptionsBuilder
import solutions.s4y.audio.mel.IMelSpectrogram
import solutions.s4y.audio.mel.KotlinMelSpectrogram
import solutions.s4y.audio.pcm.PCMAssetWavProvider
import solutions.s4y.mldemo.asr.service.whisper.Whisper
import solutions.s4y.mldemo.asr.service.whisper.WhisperTokenizer
import java.io.File
import java.io.InputStreamReader

class WhisperRule : MethodRule {
    private var initialized = false

    lateinit var context: Context

    val waveFormsAccumulator:WaveFormsAccumulator by lazy {
        WaveFormsAccumulator()
    }

    val pcmFeed: PCMFeed by lazy {
        PCMFeed()
    }

    val melSpectrogramTransformer: IMelSpectrogram by lazy { KotlinMelSpectrogram() }

    val model: Whisper by lazy {
        val modelFile: File
        runBlocking {
            File(context.filesDir, "ml").mkdirs()
            modelFile = FirebaseBlob(
                "ml/whisper-1.0.1/whisper.tflite",
                File(context.filesDir, "ml/whisper.tflite")
            ).get()
        }
        Whisper(modelFile)
    }

    val tokenizer: WhisperTokenizer by lazy {
        val tokenizerFile: File
        runBlocking {
            File(context.filesDir, "ml").mkdirs()
            tokenizerFile = FirebaseBlob(
                "ml/whisper-1.0.1/tokenizer.json",
                File(context.filesDir, "ml/whisper_tokenizer.json")
            ).get()
        }
        WhisperTokenizer(tokenizerFile)
    }

    val testPCM: ShortArray by lazy {
        val p = PCMAssetWavProvider(context, "adam/1-1.wav")
        p.shorts
    }

    val testMel: FloatArray by lazy {
        val inputStream = context.assets.open("adam/1-1-mel.json")
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
        melData.flatten().flatten().toFloatArray()
    }

    val testTokens: IntArray by lazy {
        val inputStream = context.assets.open("adam/1-1-tokens.json")
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
        tokensData.flatten().toIntArray()
    }

    val tsetTokesSet: Set<Int> by lazy {
        testTokens.toSet()
    }

    val testTranscription: String by lazy {
        val inputStream = context.assets.open("adam/1-1-transcription.txt")
        val reader = InputStreamReader(inputStream)
        val transcription = reader.readText()
        reader.close()
        transcription
    }

    override fun apply(base: Statement, method: FrameworkMethod?, target: Any?): Statement {
        val app = ApplicationProvider.getApplicationContext<Application>()
        if (!initialized) {
            val builder: FirebaseOptions.Builder = googleServicesOptionsBuilder()
            val options = builder.build()
            FirebaseApp.initializeApp(app, options, "[DEFAULT]")
            initialized = true
        }
        context = app

        return object : Statement() {
            override fun evaluate() {
                base.evaluate()
            }
        }
    }
}