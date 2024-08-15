package solutions.s4y.mldemo.voice_detection

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import solutions.s4y.audio.accumulator.PCMFeed
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import solutions.s4y.mldemo.voice_detection.yamnet.YamnetVoiceClassifier

class VoiceClassificationService(context: Context) {
    private val classificator: IVoiceClassifier
    private val _flowLabels: MutableSharedFlow<List<String>> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var jobAudioSamples: Job? = null
    private var jobClassifier: Job? = null
    private val pcmFeed = PCMFeed(YamnetVoiceClassifier.PCM_BATCH)

    init {
        classificator = YamnetVoiceClassifier(context, pcmFeed)
    }

    fun start(samplesFlow: Flow<ShortArray>, scope: CoroutineScope) {
        stop()
        jobAudioSamples = samplesFlow
            .onEach { samples ->
                pcmFeed.add(samples)
            }
            .launchIn(scope)
        jobClassifier = classificator.flow
            .onEach {
                val labels = classificator.labels(it.ids)
                val probabilities = classificator.probabilities(it.ids)
                _flowLabels.tryEmit(labels.zip(probabilities) { label, probability -> "$probability|$label" })
            }
            .launchIn(scope)
    }

    fun stop() {
        jobAudioSamples?.cancel()
        jobClassifier?.cancel()
        jobAudioSamples = null
        jobClassifier = null
    }

    val flowLabels: SharedFlow<List<String>> = _flowLabels
}