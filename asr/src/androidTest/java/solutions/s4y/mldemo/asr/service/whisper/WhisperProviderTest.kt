package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import solutions.s4y.mldemo.asr.service.rules.WhisperRule


@RunWith(Enclosed::class)
class WhisperProviderTest {
    class DecodeWaveFormsTest{
        @get:Rule
        val whisper = WhisperRule()

        @Test
        fun decodeWaveForm_shouldDecodeArabic_whenArabicWaveForm(): Unit = runBlocking {
            // Arrange
            val provider = WhisperProvider(
                whisper.waveFormsAccumulator,
                whisper.whisperTFLogMel,
                whisper.modelBaseAr,
                whisper.tokenizer
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsAr11)
            // Assert
            assertEquals(whisper.testTranscriptionAr11, decoded)
        }

    }
    /*
    @get:Rule
    val whisper = WhisperRule()

    @Ignore("Not clear what this test is supposed to do")
    @Test
    fun flow_shouldEmit(): Unit = runBlocking {
        // Arrange
        whisper.waveFormsAccumulator.batch = 16000 * 30
        val service = WhisperProvider(
            whisper.waveFormsAccumulator,
            whisper.whisperTFLogMel,
            whisper.modelAr,
            whisper.tokenizer
        )
        val job = async {
            val decoded = mutableListOf<String>()
            service.decodingFlow
                .toList(decoded)
            decoded.toList()
        }
        delay(10)
        // Act
        // do not need to close the feed, bec
        whisper.waveFormsAccumulator.add(whisper.testPCMAr11)
        whisper.waveFormsAccumulator.close()
        val decoded: List<String> = job.await()
        // Assert
        println(decoded)
    }
     */
}