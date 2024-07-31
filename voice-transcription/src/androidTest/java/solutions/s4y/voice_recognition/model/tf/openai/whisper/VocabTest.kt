package solutions.s4y.voice_recognition.model.tf.openai.whisper

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import solutions.s4y.mel.IMelSpectrogramProvider
import solutions.s4y.voice_recognition.tf.model.whisper.WhisperVoiceRecognizer

class VocabTest {
    @Test
    fun tf_shouldProvideVocab() {
        // Arrange
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val melSpectrogramProvider = object : IMelSpectrogramProvider {
            override fun getMelSpectrogram(samples: FloatArray, nSamples: Int): FloatArray {
                TODO("Not yet implemented")
            }
        }
        val whisper =
            WhisperVoiceRecognizer(context, "models/" + "openai/whisper-base.tflite", melSpectrogramProvider)
        // Act
        val text = whisper.vocab
        // Assert
        assert(text.isNotEmpty())
    }
}