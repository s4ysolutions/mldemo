package solutions.s4y.mldemo.asr.service.whisper

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import solutions.s4y.firebase.FirebaseBlob
import solutions.s4y.mldemo.asr.service.rules.WhisperRule
import java.io.File

@RunWith(Enclosed::class)
class WhisperInferrerTest {
    class ConstructorTest {
        @get:Rule
        val whisper = WhisperRule()

        /**
         * Do not use rule because this test ensures that the rule should work
         */
        @Test
        fun constructor_shouldWork_WhenFile(): Unit = runBlocking {
            // Act
            val context = ApplicationProvider.getApplicationContext<Application>()
            val modelFile: File
            runBlocking {
                File(context.filesDir, "ml").mkdirs()
                modelFile = FirebaseBlob(
                    "ml/whisper-1.0.1/whisper.tflite",
                    File(context.filesDir, "ml/whisper.tflite")
                ).get()
            }
            // no exception
            WhisperInferrer(modelFile, whisper.inferenceContext)
        }

        @Test
        fun constructor_shouldWork_WhenTestAsset(): Unit = runBlocking {
            // Arrange
            val context = ApplicationProvider.getApplicationContext<Application>()
            // Act
            WhisperInferrer(context, "ml/whisper-base-ar.tflite", whisper.inferenceContext)
            WhisperInferrer(context, "ml/whisper-base-en.tflite", whisper.inferenceContext)
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.modelFirebaseCS.toString())
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenTinyArGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.modelTinyArFirebaseCS.toString())
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenTinyEnGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.modelTinyEnFirebaseCS.toString())
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenBaseArGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.modelBaseArFirebaseCS.toString())
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenBaseEnGCS(): Unit = runBlocking {
            // Arrange && Act && Assert
            println(whisper.modelBaseEnFirebaseCS.toString())
            // Assert
            // no exception
        }

    }

    class RunInferenceTest {
        @get:Rule
        val whisper = WhisperRule()

        @Test
        fun runInference_shouldRecognizeArabicMel_whenArabicAssetModel() = runBlocking {
            // Act
            val tokens = whisper.modelBaseAr.runInference(whisper.testMelAr11).await()
            val transcription = whisper.tokenizer.decode(tokens, skipSpecial = true)
            // Assert
            assertEquals(whisper.testTokensAr11.size, tokens.size)
            assertEquals(whisper.testTranscriptionAr11, transcription)
        }

        @Test
        @Ignore("It does not work")
        fun runInference_shouldRecognizeArabicMel_whenTinyGCSArabicModel() = runBlocking {
            // Act
            val tokens = whisper.modelTinyArFirebaseCS.runInference(whisper.testMelAr11).await()
            val transcription = whisper.tokenizer.decode(tokens, skipSpecial = true)
            // Assert
            assertEquals(whisper.testTokensAr11.size, tokens.size)
            assertEquals(whisper.testTranscriptionAr11, transcription)
        }

        @Test
        fun runInference_shouldRecognizeArabicMel_whenBaseGCSArabicModel() = runBlocking {
            // Act
            val tokens = whisper.modelBaseArFirebaseCS.runInference(whisper.testMelAr11).await()
            val transcription = whisper.tokenizer.decode(tokens, skipSpecial = true)
            // Assert
            assertEquals(whisper.testTokensAr11.size, tokens.size)
            assertEquals(whisper.testTranscriptionAr11, transcription)
        }
    }
}