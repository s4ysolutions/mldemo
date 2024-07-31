package solutions.s4y.mldemo.voice_detection

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassificator
import solutions.s4y.mldemo.voice_detection.yamnet.YamnetClassifier

class VoiceClassificationService(private val audioService: AudioService) {
    private var _classificator: IVoiceClassificator? = null
    private val _classifierFlow =
        MutableSharedFlow<List<String>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun start(context: Context, scope: CoroutineScope) {
        stop()
        val scopeClassifier = CoroutineScope(scope.coroutineContext + Dispatchers.Default)
        _classificator = YamnetClassifier(context, scopeClassifier)
        audioService.samplesFlow
            .onEach { samples ->
                _classificator?.addSamples(samples)
            }.launchIn(scope)
        _classificator?.classifierFlow?.onEach { classes ->
            _classifierFlow.tryEmit(classes.first)
        }?.launchIn(scope)
    }

    fun stop() {
        _classificator?.close()
        _classificator = null
    }

    val classifierFlow: SharedFlow<List<String>> = _classifierFlow
}