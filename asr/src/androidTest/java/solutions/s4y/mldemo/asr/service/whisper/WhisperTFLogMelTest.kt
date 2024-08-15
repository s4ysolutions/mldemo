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
        // TODO: currently it almost useless
        //       just make sure it doesn't crash
        assertEquals(80 * 3000, result.size)
        assertNotEquals(0, result.fold(0.0) { acc, f -> acc + f })
    }

    companion object {
        fun <T : Number> calculateStandardDeviation(array1: FloatArray, array2: FloatArray): Double {
            var sum = 0.0
            for (i in array1.indices) {
                sum += abs(array1[i] - array2[i])
            }
            return sum / array1.size
        }
    }
}