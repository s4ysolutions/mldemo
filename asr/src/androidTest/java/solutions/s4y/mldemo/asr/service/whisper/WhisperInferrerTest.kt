package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import solutions.s4y.mldemo.asr.service.rules.WhisperRule

@RunWith(Enclosed::class)
class WhisperInferrerTest {
    class ConstructorTest {
        @get:Rule
        val whisper = WhisperRule()

        @Test
        fun constructor_shouldWork_WhenTinyArGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            whisper.gcsHuggingfaceTinyAr.toString()
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenTinyEnGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.gcsHuggingfaceTinyEn.toString())
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenBaseArGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.gcsHuggingfaceBaseAr.toString())
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenBaseEnGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.gcsHuggingfaceBaseEn.toString())
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenSergenesGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.gcsSergenesTiny.toString())
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenSergensEnGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.gcsSergenesTinyEn.toString())
            // Assert
            // no exception
        }

    }

    class RunInferenceTest {
        @get:Rule
        val whisper = WhisperRule()

        @Test
        fun runInference_shouldRecognizeArabicMel_whenTinyGCSArabicModel() = runBlocking {
            // Act
            val tokens = whisper.gcsHuggingfaceTinyAr.runInference(whisper.testMelAr11)!!
            val transcription = whisper.tokenizerHuggingface.decode(tokens, skipSpecial = true)
            // Assert
            assertEquals(whisper.testTokensAr11.size, tokens.size)
            assertEquals(whisper.testTranscriptionAr11, transcription)
        }

        @Test
        fun runInference_shouldRecognizeArabicMel_whenBaseGCSArabicModel() = runBlocking {
            // Act
            val tokens = whisper.gcsHuggingfaceBaseAr.runInference(whisper.testMelAr11)!!
            val transcription = whisper.tokenizerHuggingface.decode(tokens, skipSpecial = true)
            // Assert
            assertEquals(whisper.testTokensAr11.size, tokens.size)
            assertEquals(whisper.testTranscriptionAr11, transcription)
        }
    }
}