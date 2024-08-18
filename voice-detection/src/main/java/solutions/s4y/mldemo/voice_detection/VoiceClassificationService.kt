package solutions.s4y.mldemo.voice_detection

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.newSingleThreadContext
import solutions.s4y.audio.accumulator.PCMFeed
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import solutions.s4y.mldemo.voice_detection.yamnet.YamnetVoiceClassifier
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@Singleton
class VoiceClassificationService @Inject constructor(@ApplicationContext context: Context) {
    private val classifier: IVoiceClassifier
    private val _flowLabels: MutableSharedFlow<List<String>> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _flowClasses: MutableSharedFlow<IVoiceClassifier.Classes> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var jobAudioSamples: Job? = null
    private var jobClassifier: Job? = null

    // intermediate buffer for audio samples
    // it is necessary to feed the classifier with fixed size chunks
    private val pcmFeed = PCMFeed(YamnetVoiceClassifier.PCM_BATCH)

    init {
        val inferenceContext = newSingleThreadContext("YamnetVoiceClassifier")
        classifier = YamnetVoiceClassifier(context, pcmFeed.flow, inferenceContext)
    }

    fun start(samplesFlow: Flow<ShortArray>, scope: CoroutineScope) {
        stop()
        jobAudioSamples = samplesFlow
            .onEach { samples ->
                pcmFeed.add(samples)
            }
            .launchIn(scope)
        jobClassifier = classifier.flow
            .onEach {
                if (_flowClasses.subscriptionCount.value > 0)
                    _flowClasses.tryEmit(it)
                if (_flowLabels.subscriptionCount.value > 0) {
                    val labels = classifier.labels(it.ids)
                    val probabilities = classifier.probabilities(it.ids)
                    _flowLabels.tryEmit(labels.zip(probabilities) { label, probability -> "$probability|$label" })
                }
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
    val flowClasses: SharedFlow<IVoiceClassifier.Classes> = _flowClasses
}