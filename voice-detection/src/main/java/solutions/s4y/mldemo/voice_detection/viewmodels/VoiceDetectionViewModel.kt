package solutions.s4y.mldemo.voice_detection.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService

class VoiceDetectionViewModel() : ViewModel() {
    companion object {
        val audioService = AudioService()
        var voiceClassificationService: VoiceClassificationService? = null
        // = VoiceClassificationService(audioService)
    }

    val audioService = VoiceDetectionViewModel.audioService
    fun voiceClassificationService(context: Context): VoiceClassificationService =
        VoiceDetectionViewModel.voiceClassificationService ?: run {
            VoiceDetectionViewModel.voiceClassificationService =
                VoiceClassificationService(context, audioService)
            voiceClassificationService(context)
        }
}