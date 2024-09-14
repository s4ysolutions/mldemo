package solutions.s4y.agora.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import solutions.s4y.agora.services.AgoraVoiceRecognitionService
import solutions.s4y.agora.preferences.AppIdPreference
import solutions.s4y.agora.preferences.ChannelNamePreference
import solutions.s4y.agora.preferences.ChannelTokenPreference
import javax.inject.Inject

@HiltViewModel
class AgoraViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val recognitionService: AgoraVoiceRecognitionService,
    val appIdPreference: AppIdPreference,
    val channelNamePreference: ChannelNamePreference,
    val channelTokenPreference: ChannelTokenPreference
) : ViewModel() {
    override fun onCleared() {
        super.onCleared()
        recognitionService.close()
    }
}