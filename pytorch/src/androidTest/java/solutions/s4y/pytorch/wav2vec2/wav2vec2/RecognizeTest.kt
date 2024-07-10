package solutions.s4y.pytorch.wav2vec2.wav2vec2

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import solutions.s4y.pcm.PCM16000Resampler
import solutions.s4y.pcm.PCMAssetWavProvider
import solutions.s4y.pytorch.PCMTensorProvider
import solutions.s4y.pytorch.wav2vec2.Wav2Vec2

class RecognizeTest {
    @Test
    fun shouldRecognizePCM16() {
        // Arrange
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wav2Vec2 = Wav2Vec2(context)
        val tensorProvider =
            PCMTensorProvider(PCMAssetWavProvider(context, "OSR_us_000_0030_16k.wav"))
        // Act
        val text = wav2Vec2.recognize(tensorProvider)
        // Assert
        assert(text == "PAINT THE SOCKETS IN THE WALL DULL GREEN THE CHILD CRAWLED INTO THE DENSE GRASS BRIBES FAIL WHERE HONEST MEN WORK TRAMPLE THE SPARK ELSE THE FLAMES WILL SPREAD THE HILT OF THE SWORD WAS CARVED WITH FINE DESIGNS A ROUND HOLE WAS DRILLED THROUGH THE THIN BOARD FOOTPRINTS SHOWED THE PATH HE TOOK UP THE BEACH SHE WAS WAITING AT MY FRONT LAWN EVENT NEAR THE EDGE BROUGHT IN FRESH AIR PROD THE OLD MULE WITH A CROOKED STICK ")
    }

    @Test
    fun shouldNotRecognizePCM8() {
        // Arrange
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wav2Vec2 = Wav2Vec2(context)
        val tensorProvider =
            PCMTensorProvider(PCMAssetWavProvider(context, "OSR_us_000_0030_8k.wav"))
        // Act
        val text = wav2Vec2.recognize(tensorProvider)
        // Assert
        assert(text == "PONT THE FACT OF RAR DARDIN THE TARR CROWD INTO THE DUNSTRATH BROWN FELLER OR ANS MONRI TRAP OF A SPART ORF THE FRANES WRE SPRAIN THE HEART OF A THIRD IS CARRED WITH FINE DEVINS A ROUND HER WAS DROR THROUGH THE FUNBURAY FRITPRINTS SORED THE PATH TETOK A THEBOUT SORRITTEN AT MY FRONT ROIN A VENT NO BLODS BARTLIN FESON CROWD LER NER WIRE THE TRICKLED OF STOP ")
    }

    @Test
    fun shouldRecognizeResampledPCM8() {
        // Arrange
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wav2Vec2 = Wav2Vec2(context)
        val tensorProvider =
            PCMTensorProvider(
                PCM16000Resampler(
                    PCMAssetWavProvider(context, "OSR_us_000_0030_8k.wav"),
                    8000
                )
            )
        // Act
        val text = wav2Vec2.recognize(tensorProvider)
        // Assert
        // PROD -> PRAD
        assert(text == "PAINT THE SOCKETS IN THE WALL DULL GREEN THE CHILD CRAWLED INTO THE DENSE GRASS BRIBES FAIL WHERE HONEST MEN WORK TRAMPLE THE SPARK ELSE THE FLAMES WILL SPREAD THE HILT OF THE SWORD WAS CARVED WITH FINE DESIGNS A ROUND HOLE WAS DRILLED THROUGH THE THIN BOARD FOOTPRINTS SHOWED THE PATH HE TOOK UP THE BEACH SHE WAS WAITING AT MY FRONT LAWN EVENT NEAR THE EDGE BROUGHT IN FRESH AIR PRAD THE OLD MULE WITH A CROOKED STICK ")
    }
}