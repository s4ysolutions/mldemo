package solutions.s4y.mldemo.voice_detection.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService

class VoiceDetectionViewModel(
    context: Context,
    val audioService: AudioService,
) : ViewModel() {
    val classifier = VoiceClassificationService(context)

    companion object {
        private val audioService = AudioService()
        private var _voiceDetectionViewModel: VoiceDetectionViewModel? = null
        private fun voiceDetectionViewModel(context: Context): VoiceDetectionViewModel =
            _voiceDetectionViewModel ?: run {
                _voiceDetectionViewModel = VoiceDetectionViewModel(context, audioService)
                voiceDetectionViewModel(context)
            }
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY])
                val model = voiceDetectionViewModel(application)
                model
            }
        }
    }
}