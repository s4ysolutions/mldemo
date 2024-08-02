package solutions.s4y.mldemo.voice_detection

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassificator
import solutions.s4y.mldemo.voice_detection.yamnet.YamnetClassifier

class VoiceClassificationService(context: Context, private val audioService: AudioService) {
    private val classificator: IVoiceClassificator = YamnetClassifier(context)

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        stop()
        job = audioService.samplesFlow
            .onEach { samples ->
                classificator.addSamples(samples)
            }.launchIn(scope)
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    val labelsFlow: Flow<IVoiceClassificator.Labels> = classificator.labelsFlow
}