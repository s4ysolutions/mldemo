package solutions.s4y.mldemo.asr.service.whisper

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import solutions.s4y.mldemo.asr.service.rules.WhisperRule

@RunWith(Enclosed::class)
class EncoderDecoderTest {
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

        /**
         * Java.lang.IllegalArgumentException: Internal error:  Failed to run on the given Interpreter:
         * tensorflow/lite/kernels/reduce.cc:445 std::apply(optimized_ops::Mean<T, U>, args) was not true.
         * tensorflow/lite/kernels/reduce.cc:445 std::apply(optimized_ops::Mean<T, U>, args) was not true.
         * tensorflow/lite/kernels/reduce.cc:445 std::apply(optimized_ops::Mean<T, U>, args) was not true.
         * tensorflow/lite/kernels/reduce.cc:445 std::apply(optimized_ops::Mean<T, U>, args) was not true.
         * tensorflow/lite/kernels/reduce.cc:445 std::apply(optimized_ops::Mean<T,
         */
        @Test
        fun runInference_shouldRecognizeEnglishMel_whenTinyGCSEnglishModel() = runBlocking {
            // Act
            val tokens = whisper.gcsHuggingfaceTinyEn.transcribe(whisper.testMelEn)
            val transcription = whisper.tokenizerHuggingface.decode(tokens, skipSpecial = true)
            val expected = " Paint the sockets in the wall, dull green the child crawled into the dense grass Brides fail where honest men work Trample the spark else the flames will spread The hilt of the sword was carved with fine designs a round hole was drilled through the thin board Footprints showed the path he took up the beach"
            println("model transcribed: $transcription")
            println("         expected: ${expected}")
            // Assert
            assertEquals(whisper.testTokensEn.size, tokens.size)
            assertEquals(expected, transcription)
        }

        @Test
        fun runInference_shouldRecognizeArabicMel_whenTinyGCSArabicModel() = runBlocking {
            // Act
            val tokens = whisper.gcsHuggingfaceTinyAr.transcribe(whisper.testMelAr11)
            val transcription = whisper.tokenizerHuggingface.decode(tokens, skipSpecial = true)
            // Assert
            assertEquals(whisper.testTokensAr11.size, tokens.size)
            assertEquals(whisper.testTranscriptionAr11, transcription)
        }

        @Test
        fun runInference_shouldRecognizeArabicMel_whenBaseGCSArabicModel() = runBlocking {
            // Act
            val tokens = whisper.gcsHuggingfaceBaseAr.transcribe(whisper.testMelAr11)
            val transcription = whisper.tokenizerHuggingface.decode(tokens, skipSpecial = true)
            // Assert
            assertEquals(whisper.testTokensAr11.size, tokens.size)
            assertEquals(whisper.testTranscriptionAr11, transcription)
        }

        @Test
        fun runInference_shouldRecognizeEnglishMel_whenBaseGCSEnglishModel() = runBlocking {
            // Act
            val tokens = whisper.gcsHuggingfaceBaseEn.transcribe(whisper.testMelEn)
            val transcription = whisper.tokenizerHuggingface.decode(tokens, skipSpecial = true)
            // Assert
            assertArrayEquals(whisper.testTokensEn, tokens)
            assertEquals(" Paint the sockets in the wall, dull green the child crawled into the dense grass Brides fail where honest men work Trample the spark else the flames will spread The hilt of the sword was carved with fine designs a round hole was drilled through the thin board Footprints showed the path he took up the beach", transcription)
        }
    }
}