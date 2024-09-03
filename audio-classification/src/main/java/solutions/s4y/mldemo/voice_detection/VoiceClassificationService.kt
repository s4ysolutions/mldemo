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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import solutions.s4y.audio.batch.batch
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceClassificationService @Inject constructor(private var classifier: IVoiceClassifier) {
    private val mfClasses: MutableSharedFlow<IVoiceClassifier.Classes> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val mfLastInferencingDurations: MutableStateFlow<Int> =
        MutableStateFlow(0)
    private val mfIsReady: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    private val mfLabels: MutableSharedFlow<List<String>> =
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var jobClassifier: Job? = null

    val flowIsReady: StateFlow<Boolean> = mfIsReady
    val flowLastDuration: StateFlow<Int> = mfLastInferencingDurations

    suspend fun initialize(context: Context) {
        if (mfIsReady.value) return
        classifier.initialize(context)
        mfIsReady.value = true
    }

    fun start(samplesFlow: Flow<ShortArray>, parentScope: CoroutineScope) {
        stop()
        jobClassifier = samplesFlow
            .conflate()
            .batch(classifier.inputSize)
            .onEach { wavesForm ->
                if (mfClasses.subscriptionCount.value > 0) {
                    val classes = classifier.classify(wavesForm)
                    mfLastInferencingDurations.emit(classifier.duration)
                    mfClasses.emit(classes)
                    // avoid extra work if no subscribers
                    if (mfLabels.subscriptionCount.value > 0) {
                        val labels = classifier.labels(classes.ids)
                        val probabilities = classifier.probabilities(classes.ids)
                        mfLabels.tryEmit(labels.zip(probabilities) { label, probability -> "$probability|$label" })
                    }
                }
            }
            .launchIn(parentScope)
    }

    fun stop() {
        jobClassifier?.cancel()
        jobClassifier = null
    }

    val flowLabels: SharedFlow<List<String>> = mfLabels
    val flowClasses: SharedFlow<IVoiceClassifier.Classes> = mfClasses
}