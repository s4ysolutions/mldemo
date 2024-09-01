package solutions.s4y.mldemo.asr.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.preferences.CurrentModel
import solutions.s4y.mldemo.asr.service.ASRService
import solutions.s4y.mldemo.asr.service.whisper.EncoderDecoder
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService
import javax.inject.Inject

@HiltViewModel
class ASRViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val asrService: ASRService,
    val audioService: AudioService,
    val classifier: VoiceClassificationService,
    val currentModel: CurrentModel = CurrentModel(context)
) : ViewModel() {
    val flowBusySeconds = MutableStateFlow(0)
    var busySecondsJob: Job? = null

    private suspend fun startBusyTimer(scope: CoroutineScope) {
        flowBusySeconds.value = 0
        busySecondsJob = scope.launch {
            while (true) {
                delay(1000)
                flowBusySeconds.value++
            }
        }
    }

    private suspend fun stopBusyTimer() {
        busySecondsJob?.cancel()
        busySecondsJob = null
        flowBusySeconds.value = 0
    }

    fun trackTranscribing(scope: CoroutineScope) {
        asrService.flowIsDecoding.onEach {
            if (it) {
                startBusyTimer(scope)
            } else {
                stopBusyTimer()
            }
        }.launchIn(scope)
    }

    fun setCurrentModel(model: EncoderDecoder.Models) {
        currentModel.value = model
    }
}