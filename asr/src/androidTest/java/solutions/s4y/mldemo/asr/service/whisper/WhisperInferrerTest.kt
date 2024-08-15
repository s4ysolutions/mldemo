package solutions.s4y.mldemo.asr.service.whisper

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        /**
         * Do not use rule because this test ensures that the rule should work
         */
        @Test
        fun constructor_shouldWork_WhenFile(): Unit = runBlocking{
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
            WhisperInferrer(modelFile)
        }

        @Test
        fun constructor_shouldWork_WhenMainAsset(): Unit = runBlocking{
            // Arrange
            val context = ApplicationProvider.getApplicationContext<Application>()
            // Act
            WhisperInferrer(context, "whisper.tflite")
            // Assert
            // no exception
        }

        @Test
        fun constructor_shouldWork_WhenTestAsset(): Unit = runBlocking{
            // Arrange
            val context = ApplicationProvider.getApplicationContext<Application>()
            // Act
            WhisperInferrer(context, "ml/whisper-base-ar.tflite")
            WhisperInferrer(context, "ml/whisper-base-en.tflite")
            // Assert
            // no exception
        }
    }
    class RunInferenceTest {
        @get:Rule
        val whisper = WhisperRule()

        @Test
        fun runInference_shouldRecognizeArabicMel_whenArabicModel() {
            // Act
            val tokens = whisper.modelBaseAr.runInference(whisper.testMelAr11)
            val transcription = whisper.tokenizer.decode(tokens, skipSpecial = true)
            // Assert
            assertEquals(whisper.testTokensAr11.size, tokens.size)
            assertEquals(whisper.testTranscriptionAr11, transcription)
        }
    }
}