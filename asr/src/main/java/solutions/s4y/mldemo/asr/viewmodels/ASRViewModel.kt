package solutions.s4y.mldemo.asr.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService

class ASRViewModel() : ViewModel() {
    companion object {
        val audioService = AudioService()
        var voiceClassificationService: VoiceClassificationService? = null
    }

    val audioService = ASRViewModel.audioService
    fun voiceClassificationService(context: Context): VoiceClassificationService =
        ASRViewModel.voiceClassificationService ?: run {
            ASRViewModel.voiceClassificationService =
                VoiceClassificationService(context, audioService)
            voiceClassificationService(context)
        }
}