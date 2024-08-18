package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import solutions.s4y.mldemo.asr.service.rules.WhisperRule


@RunWith(Enclosed::class)
class WhisperPipelineTest {

    class DecodeWaveFormsTest{
        @get:Rule
        val whisper = WhisperRule()

        @Test
        fun decodeWaveForm_shouldDecodeArabic_whenArabicWaveForm(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.whisperTFLogMel,
                whisper.modelBaseAr,
                whisper.tokenizer,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsAr11)
            // Assert
            // assertEquals(whisper.testTranscriptionAr11WithError, decoded)
            // works on real device
            assertEquals(whisper.testTranscriptionAr11, decoded)
        }

        @Test
        fun decodeWaveForm_shouldDecodeEnglish_whenTinyModel(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.whisperTFLogMel,
                whisper.modelTiny,
                whisper.tokenizer,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            // assertEquals(whisper.testTranscriptionAr11WithError, decoded)
            // works on real device
            assertEquals(whisper.testTranscriptionAr11, decoded)
        }

        @Test
        fun decodeWaveForm_shouldDecodeEnglish_whenTinyEnEnModel(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.whisperTFLogMel,
                whisper.modelTiny,
                whisper.tokenizer,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            // assertEquals(whisper.testTranscriptionAr11WithError, decoded)
            // works on real device
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