package solutions.s4y.mldemo.asr.whisper

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import solutions.s4y.mldemo.asr.whisper.rules.WhisperRule

class WhisperTest {
    @get:Rule
    val whisper = WhisperRule()

    @Test
    fun runInference_shouldRecognize_adam1_1() {
        // Act
        val tokens = whisper.model.runInference(whisper.testMel)
        // Assert
        var count = 0
        tokens.forEach {
            if (whisper.tsetTokesSet.contains(it)) {
                count++
            }
        }
        assertEquals(whisper.testTokens.size, count)
    }
}