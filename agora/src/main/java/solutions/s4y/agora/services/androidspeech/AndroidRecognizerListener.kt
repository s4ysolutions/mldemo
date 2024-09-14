package solutions.s4y.agora.services.androidspeech

import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log

// Just simplify the callbacks
internal class AndroidRecognizerListener(
    private val onReady: () -> Unit,
    private val onError: (code: Int, message: String) -> Unit,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val tag: String? = null,

    ) : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) {
        if (tag != null) Log.d(tag, "onReadyForSpeech")
        onReady()
    }

    override fun onBeginningOfSpeech() {
        if (tag != null) Log.d(tag, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // if (tag!=null) Log.d(tag, "onRmsChanged")
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        if (tag != null) Log.d(tag, "onBufferReceived")
    }

    override fun onEndOfSpeech() {
        if (tag != null) Log.d(tag, "onEndOfSpeech")
    }

    override fun onError(error: Int) {
        val msg = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
        Log.e(tag, "Speech recognition error($error): $msg")
        onError(error, msg)
    }

    override fun onLanguageDetection(results: Bundle) {
        if (tag != null) Log.d(tag, "onLanguageDetection ${results.getString("detected_language")}")
    }

    override fun onSegmentResults(segmentResults: Bundle) {
        if (tag != null) Log.d(
            tag, "android.speech: onSegmentResults ${
                segmentResults.keySet().joinToString()
            }"
        )
    }

    override fun onEndOfSegmentedSession() {
        if (tag != null) Log.d(tag, "onEndOfSegmentedSession")
    }

    override fun onResults(results: Bundle?) {
        val result =
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        onResult(result ?: "")
        if (tag != null) Log.d(tag, "onResults=$result")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val currentLocale = partialResults?.getString("current_locale")
        val resultsRecognition =
            partialResults?.getStringArrayList("results_recognition")?.joinToString()
        val unstableText =
            partialResults?.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
                ?.joinToString() ?: ""

        if (tag != null) {
            Log.d(tag, "android.speech: onPartialResults currentLocale=$currentLocale")
            Log.d(tag, "android.speech: onPartialResults resultsRecognition=$resultsRecognition")
            Log.d(tag, "android.speech: onPartialResults unstableText=$unstableText")
        }

        val result = (resultsRecognition + unstableText).trim()
        if (result.isNotEmpty()) {
            onPartial(result)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        if (tag != null) Log.d(tag, "onEvent $eventType")
    }
}