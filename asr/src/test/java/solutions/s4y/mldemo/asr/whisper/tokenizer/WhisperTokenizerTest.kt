package solutions.s4y.mldemo.asr.whisper.tokenizer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import solutions.s4y.mldemo.asr.service.whisper.WhisperTokenizer
import solutions.s4y.mldemo.asr.whisper.tokenizer.fixtures.Fixtures

class WhisperTokenizerTest : Fixtures {

    @Test
    fun decode_shouldSkipSpecial_whenCompact() {
        // Arrange
        val tokenizer = WhisperTokenizer(tokenizerJson)
        // Act
        val decoded =
            tokenizer.decode(tokens1_1, skipSpecial = true, compactSameSpecialTokens = true)
        // Assert
        assertEquals(transcription1_1, decoded)
    }

    @Test
    fun decode_shouldSkipSpecial_whenNotCompact() {
        // Arrange
        val tokenizer = WhisperTokenizer(tokenizerJson)
        // Act
        val decoded =
            tokenizer.decode(tokens1_1, skipSpecial = true, compactSameSpecialTokens = false)
        // Assert
        assertEquals(transcription1_1, decoded)
    }

    @Test
    fun decode_shouldNotSkipSpecial1_1() {
        // Arrange
        val tokenizer = WhisperTokenizer(tokenizerJson)
        // Act
        val decoded =
            tokenizer.decode(tokens1_1, skipSpecial = false, compactSameSpecialTokens = false)
        // Assert
        val prefix = "<|startoftranscript|><|ar|><|transcribe|><|notimestamps|>"
        val suffix = "<|endoftext|><|endoftext|><|endoftext|>"
        assertTrue(decoded.startsWith(prefix))
        assertTrue(decoded.endsWith(suffix))
    }

    @Test
    fun decode_shouldCompactSpecial1_1() {
        // Arrange
        val tokenizer = WhisperTokenizer(tokenizerJson)
        // Act
        val decoded =
            tokenizer.decode(tokens1_1, skipSpecial = false, compactSameSpecialTokens = true)
        // Assert
        val prefix = "<|startoftranscript|><|ar|><|transcribe|><|notimestamps|>"
        val suffix = " <|endoftext|>"
        assertEquals(
            "<|startoftranscript|><|ar|><|transcribe|><|notimestamps|>${transcription1_1}<|endoftext|>",
            decoded
        )
    }
}