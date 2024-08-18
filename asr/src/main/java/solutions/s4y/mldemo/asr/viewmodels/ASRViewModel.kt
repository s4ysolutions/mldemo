package solutions.s4y.mldemo.asr.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.launch
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.service.ASRService
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class ASRViewModel @Inject constructor(
    val asrService: ASRService,
    val audioService: AudioService,
    val classifier: VoiceClassificationService,
) : ViewModel()