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
        fun decodeWaveForm_shouldDecodeArabic_whenHuggingfaceBaseAr(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.assetWhisperTFLogMel,
                whisper.gcsHuggingfaceBaseAr,
                whisper.tokenizerHuggingface,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsAr11)
            // Assert
            assertEquals(whisper.testTranscriptionAr11WithError, decoded)
        }

        @Test
        fun decodeWaveForm_shouldDecodeEnglish_whenHuggingfaceTinyEn(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.assetWhisperTFLogMel,
                whisper.gcsHuggingfaceTinyEn,
                whisper.tokenizerHuggingface,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            assertEquals(whisper.testTranscriptionEnTimestamp, decoded)
        }

        @Test
        fun decodeWaveForm_shouldDecodeEnglish_whenSergenesTiny(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.assetWhisperTFLogMel,
                whisper.gcsSergenesTiny,
                whisper.tokenizerHuggingface,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            assertEquals(whisper.testTranscriptionEnTimestamp, decoded)
        }

        @Test
        fun decodeWaveForm_shouldDecodeEnglish_whenSergenesEnEnModel(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.assetWhisperTFLogMel,
                whisper.gcsSergenesTinyEn,
                whisper.tokenizerHuggingface,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            assertEquals(whisper.testTranscriptionEnTimestamp, decoded)
        }
    }
}