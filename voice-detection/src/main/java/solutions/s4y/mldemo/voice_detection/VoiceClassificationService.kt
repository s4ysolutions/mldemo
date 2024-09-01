package solutions.s4y.mldemo.voice_detection

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import solutions.s4y.audio.batch.batch
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceClassificationService @Inject constructor(private var classifier: IVoiceClassifier) {
    private val _flowLabels: MutableSharedFlow<List<String>> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _flowClasses: MutableSharedFlow<IVoiceClassifier.Classes> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val _flowDurations: MutableStateFlow<Long> = MutableStateFlow(0)

    private var jobAudioSamples: Job? = null
    private var jobClassifier: Job? = null
    private val mutableFlowReady: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    val flowReady: StateFlow<Boolean> = mutableFlowReady
    val flowDuration: StateFlow<Long> = _flowDurations

    suspend fun initialize(context: Context) {
        if (mutableFlowReady.value) return
        classifier.initialize(context)
        mutableFlowReady.value = true
    }

    fun start(samplesFlow: Flow<ShortArray>, scope: CoroutineScope) {
        stop()
        jobAudioSamples = samplesFlow
            // conflate?
            .batch(classifier.inputSize)
            .onEach { wavesForm ->
                if (_flowClasses.subscriptionCount.value > 0) {
                    val classes = classifier.classify(wavesForm)
                    _flowDurations.emit(classifier.duration)
                    _flowClasses.emit(classes)
                    // avoid extra work if no subscribers
                    if (_flowLabels.subscriptionCount.value > 0) {
                        val labels = classifier.labels(classes.ids)
                        val probabilities = classifier.probabilities(classes.ids)
                        _flowLabels.tryEmit(labels.zip(probabilities) { label, probability -> "$probability|$label" })
                    }
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