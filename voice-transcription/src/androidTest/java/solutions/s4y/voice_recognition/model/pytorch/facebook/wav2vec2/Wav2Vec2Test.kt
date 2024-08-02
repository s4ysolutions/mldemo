package solutions.s4y.voice_recognition.model.pytorch.facebook.wav2vec2
/*
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.pytorch.LitePyTorchAndroid
import solutions.s4y.pcm.PCM16000Resampler
import solutions.s4y.pcm.PCMAssetWavProvider
import solutions.s4y.mldemo.voice_transcription.obsolete.pytorch.model.wav2vec2.Wav2vec2VoiceRecognizer

@RunWith(Enclosed::class)
class Wav2Vec2Test {
    @RunWith(Parameterized::class)
    class RecognizeTest(private val title: String, private val modelAsset: String, private val pcm: FloatArray, private val expected: String) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "({0})")
            fun data() = listOf(
                arrayOf(
                    "PCM16 Base 960h",
                    "facebook/wav2vec2-base-960h.ptl",
                    PCMAssetWavProvider(InstrumentationRegistry.getInstrumentation().targetContext, "OSR_us_000_0030_16k.wav").floats,
                    "PAINT THE SOCKETS IN THE WALL DULL GREEN THE CHILD CRAWLED INTO THE DENSE GRASS BRIBES FAIL WHERE HONEST MEN WORK TRAMPLE THE SPARK ELSE THE FLAMES WILL SPREAD THE HILT OF THE SWORD WAS CARVED WITH FINE DESIGNS A ROUND HOLE WAS DRILLED THROUGH THE THIN BOARD FOOTPRINTS SHOWED THE PATH HE TOOK UP THE BEACH SHE WAS WAITING AT MY FRONT LAWN EVENT NEAR THE EDGE BROUGHT IN FRESH AIR PROD THE OLD MULE WITH A CROOKED STICK "
                ),
                arrayOf(
                    "PCM8 Base 960h",
                    "facebook/wav2vec2-base-960h.ptl",
                    PCMAssetWavProvider(InstrumentationRegistry.getInstrumentation().targetContext, "OSR_us_000_0030_8k.wav").floats,
                    "PONT THE FACT OF RAR DARDIN THE TARR CROWD INTO THE DUNSTRATH BROWN FELLER OR ANS MONRI TRAP OF A SPART ORF THE FRANES WRE SPRAIN THE HEART OF A THIRD IS CARRED WITH FINE DEVINS A ROUND HER WAS DROR THROUGH THE FUNBURAY FRITPRINTS SORED THE PATH TETOK A THEBOUT SORRITTEN AT MY FRONT ROIN A VENT NO BLODS BARTLIN FESON CROWD LER NER WIRE THE TRICKLED OF STOP "
                    ),
                arrayOf(
                    "PCM16 resampled Base 960h",
                    "facebook/wav2vec2-base-960h.ptl",
                    PCM16000Resampler(PCMAssetWavProvider(InstrumentationRegistry.getInstrumentation().targetContext, "OSR_us_000_0030_8k.wav"), 8000).floats,
                    // PROD -> PRAD
                    "PAINT THE SOCKETS IN THE WALL DULL GREEN THE CHILD CRAWLED INTO THE DENSE GRASS BRIBES FAIL WHERE HONEST MEN WORK TRAMPLE THE SPARK ELSE THE FLAMES WILL SPREAD THE HILT OF THE SWORD WAS CARVED WITH FINE DESIGNS A ROUND HOLE WAS DRILLED THROUGH THE THIN BOARD FOOTPRINTS SHOWED THE PATH HE TOOK UP THE BEACH SHE WAS WAITING AT MY FRONT LAWN EVENT NEAR THE EDGE BROUGHT IN FRESH AIR PRAD THE OLD MULE WITH A CROOKED STICK "
                    )
            )
        }

        @Ignore("not supported")
        @Test
        fun shouldRecognize() {
            // Arrange
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val module = LitePyTorchAndroid.loadModuleFromAsset(context.assets, "models/"+modelAsset)
            val wav2Vec2 = Wav2vec2VoiceRecognizer(module)
            // Act
            val text = wav2Vec2.recognize(pcm)
            // Assert
            assertEquals(expected.replace(" ", "|"), text)
        }
    }
}
 */