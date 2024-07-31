package solutions.s4y.mldemo.voice_detection.viewmodels

import androidx.lifecycle.ViewModel
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService

class VoiceDetectionViewModel: ViewModel() {
    companion object {
        val audioService = AudioService()
        val voiceClassificationService = VoiceClassificationService(audioService)
    }
    val audioService = VoiceDetectionViewModel.audioService
    val voiceClassificationService = VoiceDetectionViewModel.voiceClassificationService
}