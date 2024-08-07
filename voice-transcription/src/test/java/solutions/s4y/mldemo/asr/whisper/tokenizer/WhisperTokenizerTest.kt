package solutions.s4y.mldemo.asr.whisper.tokenizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import solutions.s4y.mldemo.asr.whisper.WhisperTokenizer
import solutions.s4y.mldemo.asr.whisper.tokenizer.fixtures.Fixtures

class WhisperTokenizerTest : Fixtures {

    @Test
    fun decode_shouldDecode1_1() {
        // Arrange
        val tokenizer = WhisperTokenizer(tokenizerJson)
        // Act
        val decoded = tokenizer.decode(tokens1_1, skipSpecial = true)
        // Assert
        assertEquals(transcription1_1, decoded)
    }
}