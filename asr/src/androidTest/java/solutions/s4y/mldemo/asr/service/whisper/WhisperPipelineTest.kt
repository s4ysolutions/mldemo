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

    class DecodeWaveFormsTest {
        @get:Rule
        val whisper = WhisperRule()

        @Test
        fun decodeWaveForm_shouldDecodeArabic_whenHuggingfaceBaseEn(): Unit = runBlocking {
            // Arrange
            val pipeline = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.assetTFLiteLogMel,
                whisper.gcsHuggingfaceBaseEn,
                whisper.tokenizerHuggingface,
            )
            // Act
            val decoded = pipeline.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            assertEquals(
                " Paint the sockets in the wall, dull green the child crawled into the dense grass Brides fail where honest men work Trample the spark else the flames will spread The hilt of the sword was carved with fine designs a round hole was drilled through the thin board Footprints showed the path he took up the beach",
                decoded
            )
        }

        @Test
        fun decodeWaveForm_shouldDecodeArabic_whenHuggingfaceBaseAr(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.assetTFLiteLogMel,
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
                whisper.assetTFLiteLogMel,
                whisper.gcsHuggingfaceTinyEn,
                whisper.tokenizerHuggingface,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            assertEquals(whisper.testTranscriptionEnTinyTimestamp, decoded)
        }

        @Test
        fun decodeWaveForm_shouldDecodeEnglish_whenSergenesTiny(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.assetTFLiteLogMel,
                whisper.gcsSergenesTiny,
                whisper.tokenizerHuggingface,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            assertEquals(whisper.testTranscriptionEnTinyTimestamp, decoded)
        }

        @Test
        fun decodeWaveForm_shouldDecodeEnglish_whenSergenesEnEnModel(): Unit = runBlocking {
            // Arrange
            val provider = WhisperPipeline(
                whisper.waveFormsAccumulator.flow,
                whisper.assetTFLiteLogMel,
                whisper.gcsSergenesTinyEn,
                whisper.tokenizerHuggingface,
            )
            // Act
            val decoded = provider.decodeWaveForms(whisper.testWaveFormsEn)
            // Assert
            assertEquals(whisper.testTranscriptionEnTinyTimestamp, decoded)
        }
    }
}