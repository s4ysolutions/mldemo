package solutions.s4y.mldemo.asr.service.whisper

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import solutions.s4y.mldemo.asr.service.accumulator.Accumulator16000
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "accumulate16000"
private const val NON_SPEECH_THRESHOLD = 3
private const val MIN_DURATION = 1 // do not emit less than
private const val MAX_DURATION = 2 // force emit after this duration

enum class SpeechState {
    IDLE,
    NON_SPEECH,
    SPEECH
}


fun Flow<IVoiceClassifier.Classes>.accumulate16000(
    flowAccumulatorSize: MutableStateFlow<Int>,
): Flow<FloatArray> =
    flow {
        val nonSpeechCount = AtomicInteger(0)
        val accumulator16000 = Accumulator16000()

        // TODO: lambdas should be avoided somehow?
        suspend fun isMinDuration() = accumulator16000.duration() > MIN_DURATION
        suspend fun isMaxDuration() = accumulator16000.duration() > MAX_DURATION

        flowAccumulatorSize.value = 0
        collect {
            try {
                val cls = it.ids[0] // for debut purposes

                if (it.isSpeech) {
                    nonSpeechCount.set(0)
                    accumulator16000.add(it.waveForms)
                    Log.d(
                        TAG,
                        "Speech cls=$cls detected for ${accumulator16000.duration()}s, emit = ${isMaxDuration()}"
                    )
                    // continued speech should not be transcribed immediately
                    // wait either for specific duration or for silence
                    if (isMaxDuration()) {
                        emit(accumulator16000.waveForms())
                    }
                    return@collect
                }

                // Non-speech
                val count = nonSpeechCount.incrementAndGet()
                @Suppress("KotlinConstantConditions")
                when {
                    count < NON_SPEECH_THRESHOLD -> {
                        // skip short pause
                        Log.d(
                            TAG,
                            "Non speech cls=$cls detected $count times (short pause) , accumulated=${accumulator16000.duration()}s"
                        )
                    }

                    count == NON_SPEECH_THRESHOLD -> {
                        Log.d(
                            TAG,
                            "Non speech cls=$cls detected $count times, (emit = ${isMinDuration()}), accumulated=${accumulator16000.duration()}s"
                        )
                        if (isMinDuration() && !accumulator16000.isEmpty()) {
                            val success = emit(accumulator16000.waveForms())
                            Log.d(TAG, "emit waveforms: $success")
                        }
                        accumulator16000.reset()
                        //TODO: clear the queue - hack
                        emit(FloatArray(0))
                    }

                    count > NON_SPEECH_THRESHOLD -> {
                        //TODO: ugly, should never be needed, but just in case
                        accumulator16000.reset()
                        Log.d(
                            TAG,
                            "Non speech cls=$cls detected $count times, (long pause), accumulated=${accumulator16000.duration()}s"
                        )
                    }
                }
            } finally {
                if (accumulator16000.size() < 100) {
                    flowAccumulatorSize.value = 0
                } else {
                    flowAccumulatorSize.value = (accumulator16000.size() / 16000 + .5).toInt()
                }
            }
        }
    }