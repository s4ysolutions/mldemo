package solutions.s4y.mldemo.asr.service.whisper

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import solutions.s4y.mldemo.asr.service.rules.WhisperRule

class WhisperTokenizerTest {
    @get:Rule
    val whisper = WhisperRule()

    @Test
    fun decode_shouldDecode1_1() {
        // Arrange
        val tokenizer = whisper.tokenizerHuggingface
        // Act
        val decoded = tokenizer.decode(whisper.testTokensAr11, skipSpecial = true)
        // Assert
        assertEquals(whisper.testTranscriptionAr11, decoded)
    }
}