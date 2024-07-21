package solutions.s4y.voice_recognition.model.tf.openai.whisper

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import solutions.s4y.mel.KotlinMelSpectrogramProvider
import solutions.s4y.pcm.PCMAssetWavProvider
import solutions.s4y.voice_recognition.tf.model.whisper.WhisperVoiceRecognizer

@RunWith(Enclosed::class)
class WhisperVoiceTest {
    @RunWith(Parameterized::class)
    class RecognizeTest(
        private val title: String,
        private val modelAsset: String,
        private val pcm: FloatArray,
        private val expected: String
    ) {
        // TODO: decoding from tokens to text is not implemented yet
        //       so the expected result is set to empty string
        //       But the test confirms the Interperter can be loaded and run without any errors
        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "({0})")
            fun data() = listOf(
                arrayOf(
                    "1:1",
                    "openai/whisper-base.tflite",
                    PCMAssetWavProvider(InstrumentationRegistry.getInstrumentation().targetContext, "adam/1-1.wav").floats,
                    ""
                ),
                arrayOf(
                    "3:14",
                    "openai/whisper-base.tflite",
                    PCMAssetWavProvider(InstrumentationRegistry.getInstrumentation().targetContext, "adam/3-14.wav").floats,
                    ""
                ),
            )
        }
        @Test
        fun shouldRecognize() {
            // Arrange
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val melSpectrogramProvider = KotlinMelSpectrogramProvider()
            val whisper =
                WhisperVoiceRecognizer(context, "models/" + modelAsset, melSpectrogramProvider)
            // Act
            val text = whisper.recognize(pcm)
            // Assert
            assertEquals(expected.replace(" ", "|"), text)
        }
    }
}