package solutions.s4y.mldemo.asr.viewmodels

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.preferences.CurrentModel
import solutions.s4y.mldemo.asr.service.ASRService
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService
import javax.inject.Inject
import javax.inject.Singleton

@HiltViewModel
class ASRViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val asrService: ASRService,
    val audioService: AudioService,
    val classifier: VoiceClassificationService,
    val currentModel: CurrentModel = CurrentModel(context)
) : ViewModel()