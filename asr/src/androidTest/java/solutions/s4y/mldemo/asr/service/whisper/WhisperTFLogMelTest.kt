package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import solutions.s4y.mldemo.asr.service.rules.WhisperRule

class WhisperTFLogMelTest {
    @get:Rule
    val whisper: WhisperRule = WhisperRule()

    @Test
    fun getMelSpectrogram_shouldReturn3000frames80bins_whenArabic() = runBlocking {
        // Arrange
        val logMelSpectrogram =
            WhisperTFLogMel(whisper.context, "features-extractor.tflite", whisper.inferenceContext)
        // Act
        val result = logMelSpectrogram.getMelSpectrogram(whisper.testWaveFormsAr11).await()
        // Assert
        // TODO: currently it almost useless
        //       just make sure it doesn't crash
        assertEquals(80 * 3000, result.size)
        assertNotEquals(0, result.fold(0.0) { acc, f -> acc + f })
    }
}