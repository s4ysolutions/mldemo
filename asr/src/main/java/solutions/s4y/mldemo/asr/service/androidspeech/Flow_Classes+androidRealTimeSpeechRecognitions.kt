package solutions.s4y.mldemo.asr.service.androidspeech

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognizerIntent
import android.speech.RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION
import android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS
import android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS
import android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS
import android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import java.util.concurrent.atomic.AtomicBoolean


private const val TAG = "AndroidSpeechRecognizer"

@SuppressLint("InlinedApi")
private fun speechRecognizerIntent(audioPipeOutput: ParcelFileDescriptor): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(EXTRA_ENABLE_LANGUAGE_DETECTION, true)
        putExtra(EXTRA_PARTIAL_RESULTS, true)
        putExtra(EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
        putExtra(EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
        putExtra(EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000)
        putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audioPipeOutput);

        //putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        /*
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale("en").language
        )
         */
    }

fun Flow<IVoiceClassifier.Classes>.androidRealTimeSpeechRecognitions(
    context: Context,
    onVoiceDetected: () -> Unit,
    onRecognizerStop: () -> Unit
): Flow<String> = callbackFlow {
    // TODO: close FDs and pipes
    val audioPipe: Array<ParcelFileDescriptor> = ParcelFileDescriptor.createPipe();
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    var mOutputStream: ParcelFileDescriptor.AutoCloseOutputStream =
        ParcelFileDescriptor.AutoCloseOutputStream(audioPipe[1])

    val isListening = AtomicBoolean(false)
    speechRecognizer.setRecognitionListener(object :
        android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "android.speech: onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "android.speech: onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // println("android.speech: onRmsChanged $rmsdB")
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.d(TAG, "android.speech: onBufferReceived")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "android.speech: onEndOfSpeech")
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
            Log.e(TAG, "Speech recognition error($error): $msg")
            isListening.set(false)
            onRecognizerStop()
        }

        override fun onLanguageDetection(results: Bundle) {
            Log.d(
                TAG,
                "android.speech: onLanguageDetection ${results.getString("detected_language")}"
            )
        }

        override fun onSegmentResults(segmentResults: Bundle) {
            Log.d(
                TAG,
                "android.speech: onSegmentResults ${
                    segmentResults.keySet().joinToString()
                }"
            )
        }

        override fun onEndOfSegmentedSession() {
            Log.d(TAG, "android.speech: onEndOfSegmentedSession")
        }

        override fun onResults(results: Bundle?) {
            Log.d(TAG, "android.speech: onResults")
            val result =
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
            if (result != null) {
                trySend(result)
            }
            isListening.set(false)
            onRecognizerStop()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val currentLocale = partialResults?.getString("current_locale")
            val resultsRecognition =
                partialResults?.getStringArrayList("results_recognition")
                    ?.joinToString()
            val unstableText =
                partialResults?.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
                    ?.joinToString()
            Log.d(TAG, "android.speech: onPartialResults currentLocale=$currentLocale")
            Log.d(TAG, "android.speech: onPartialResults resultsRecognition=$resultsRecognition")
            Log.d(TAG, "android.speech: onPartialResults unstableText=$unstableText")

            val result = (resultsRecognition + unstableText).trim()
            if (result.isNotEmpty()) {
                trySend(result)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "android.speech: onEvent")
        }
    })
    var nonSpeechCount = 0
    try {
        this@androidRealTimeSpeechRecognitions.collect {
            if (it.isSpeech) {
                Log.d(TAG, "voice detected")
                onVoiceDetected()
                val wasListening = isListening.getAndSet(true)
                // if there were more than 5 non-speech events,restart listening0
                if (!wasListening || nonSpeechCount > 5) {
                    Log.d(TAG, " speechRecognizer.startListening")
                    speechRecognizer.startListening(speechRecognizerIntent(audioPipe[0]))
                }
                it.waveForms.forEach { waveForm ->
                //    mOutputStream.write(waveForm)
                }
                nonSpeechCount = 0
            } else {
                Log.d(TAG, "no voice detected")
                nonSpeechCount++
            }
        }
    } finally {
        Log.d(TAG, "android.speech: close")
        speechRecognizer.destroy()
    }
}