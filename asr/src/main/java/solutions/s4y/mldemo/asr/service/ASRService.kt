package solutions.s4y.mldemo.asr.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.shareIn
import solutions.s4y.mldemo.asr.service.whisper.WhisperProvider
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService

class ASRService(
    private val voiceClassificationService: VoiceClassificationService,
    private val whisperProvider: WhisperProvider,
) {
    private val flowShareInScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    //private val vcSharedFlow = voiceClassificationService.flowLabels.shareIn(flowShareInScope)
}