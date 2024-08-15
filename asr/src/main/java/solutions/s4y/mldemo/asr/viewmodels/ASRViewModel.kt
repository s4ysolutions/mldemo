package solutions.s4y.mldemo.asr.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.Flow
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService

class ASRViewModel() : ViewModel() {
    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ASRViewModel()
            }
        }
    }
}