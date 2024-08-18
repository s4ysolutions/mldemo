package solutions.s4y.mldemo.asr.service.rules

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.gson.JsonParser
import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import solutions.s4y.audio.pcm.PCMAssetWavProvider
import java.io.InputStreamReader

class PcmRule : MethodRule {

    lateinit var context: Context

    val testPCMAr11: ShortArray by lazy {
        val p = PCMAssetWavProvider(context, "adam/1-1.wav")
        p.shorts
    }

    val testWaveFormsAr11: FloatArray by lazy {
        val p = PCMAssetWavProvider(context, "adam/1-1.wav")
        p.floats
    }

    val testMelAr11: FloatArray by lazy {
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

    override fun apply(base: Statement, method: FrameworkMethod?, target: Any?): Statement {
        val app = ApplicationProvider.getApplicationContext<Application>()
        context = app

        return object : Statement() {
            override fun evaluate() {
                base.evaluate()
            }
        }
    }
}