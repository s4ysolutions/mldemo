package solutions.s4y.mldemo.voice_detection.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.voice_detection.VoiceClassificationService
import javax.inject.Inject


@HiltViewModel
class VoiceDetectionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    val audioService: AudioService,
    val classifier: VoiceClassificationService
) : ViewModel() {
}