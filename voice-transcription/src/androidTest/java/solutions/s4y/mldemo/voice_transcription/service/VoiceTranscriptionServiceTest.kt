package solutions.s4y.mldemo.voice_transcription.service

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import solutions.s4y.firebase.FirebaseBlob
import solutions.s4y.mldemo.voice_transcription.service.implementation.KotlinMelSpectrogramTransformer
import solutions.s4y.mldemo.voice_transcription.service.implementation.MemoryWaveFormsAccumulator
import solutions.s4y.mldemo.voice_transcription.service.implementation.WhisperVoiceTranscriber
import solutions.s4y.voice_transcription.rules.FirebaseRule
import java.io.File


class VoiceTranscriptionServiceTest {
    @get:Rule
    val firebaseRule = FirebaseRule()

    private fun modelFile(context: Context): File = runBlocking {
        File(context.filesDir, "ml").mkdirs()
        FirebaseBlob(
            "ml/whisper-1.0.1/whisper.tflite",
            File(context.filesDir, "ml/whisper.tflite")
        ).get()
    }

    private fun service(context: Context): VoiceTranscriptionService {
        val modelFile = modelFile(context)
        val melSpectrogramTransformer = KotlinMelSpectrogramTransformer()
        val transcriber = WhisperVoiceTranscriber(modelFile)
        val accumulator = MemoryWaveFormsAccumulator()
        return VoiceTranscriptionService(
            accumulator = accumulator,
            melSpectrogramProvider = melSpectrogramTransformer,
            transcriber = transcriber)
    }

    @Test
    fun service() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = service(context)
    }

}