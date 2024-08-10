package solutions.s4y.mldemo.asr.service.whisper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import solutions.s4y.mldemo.asr.service.rules.WhisperRule
import kotlin.math.abs

class WhisperTFLogMelTest {
    @get:Rule
    val whisper: WhisperRule = WhisperRule()

    @Test
    fun getMelSpectrogram_shouldReturn3000frames80bins_whenArabic() {
        // Arrange
        val logMelSpectrogram = WhisperTFLogMel(whisper.context, "features-extractor.tflite")
        // Act
        val result = logMelSpectrogram.getMelSpectrogram(whisper.testWaveFormsAr11)
        // Assert
        assertEquals(80 * 3000, result.size)
        assertNotEquals(0, result.fold(0.0) { acc, f -> acc + f })
        var diffCounts = 0
        for (i in 0 until 80) {
            for (j in 0 until 3000) {
                if (abs(whisper.testMelAr11[i * 3000 + j] - result[i * 3000 + j]) > 1f) {
                    diffCounts++
                }
            }
        }
        // 1 bin per frame
        assertTrue("More the 1 wrong bin per 100 frames", diffCounts < 30)
    }
}