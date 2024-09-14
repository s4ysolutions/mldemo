package solutions.s4y.agora.services.androidspeech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION
import android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS
import android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS
import android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS
import android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.intl.Locale
import java.io.Closeable

class AndroidRecognizerWithStream(
    context: Context,
    private val tag: String? = null,
    recognizerListener: RecognitionListener,
) : Closeable {
    private val communicationPipe: Array<ParcelFileDescriptor> = ParcelFileDescriptor.createPipe()
    private val readFD: ParcelFileDescriptor get() = communicationPipe[0]
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(recognizerListener)
    }

    val writeStream = ParcelFileDescriptor.AutoCloseOutputStream(communicationPipe[1])

    override fun close() {
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
        writeStream.close()
        readFD.close()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun startListening(language: String) {
        val speechRecognizerIntent = getIntent()
        val langCode = Locale(language).language
        if (tag != null) {
            Log.d(tag, "startListening: $langCode")
        }
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale(language).language
        )
        speechRecognizer.startListening(speechRecognizerIntent)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun startListening() {
        val speechRecognizerIntent = getIntent()
        speechRecognizerIntent.putExtra(EXTRA_ENABLE_LANGUAGE_DETECTION, true)
        speechRecognizer.startListening(speechRecognizerIntent)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(EXTRA_PARTIAL_RESULTS, true)
        putExtra(EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        putExtra(EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
        putExtra(EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readFD)
    }
}