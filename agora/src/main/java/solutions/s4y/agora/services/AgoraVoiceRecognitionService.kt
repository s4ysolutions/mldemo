package solutions.s4y.agora.services

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import solutions.s4y.agora.preferences.LanguagePreference
import solutions.s4y.agora.services.agora.AgoraPCMProvider
import solutions.s4y.agora.services.androidspeech.AndroidRecognizer
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AgoraVoiceRecognitionService @Inject constructor(
    @ApplicationContext val context: Context,
    val languagePreference: LanguagePreference,
) : Closeable {
    private val mutableFlowTranscriptions = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val androidRecognizer = AndroidRecognizer(context, languagePreference, TAG) {
        mutableFlowTranscriptions.tryEmit(it)
    }

    private val agora =
        AgoraPCMProvider(TAG) @RequiresApi(Build.VERSION_CODES.TIRAMISU) { buffer ->
            launchInMainScope {
                androidRecognizer.addPcm(buffer)
            }
        }

    val flowAngoraChannelState = agora.stateFlow
    val flowTranscriptions: SharedFlow<String> = mutableFlowTranscriptions

    override fun close() {
        androidRecognizer.close()
        agora.close()
    }

    fun joinChannel(context: Context) {
        agora.joinChannel(context)
    }

    fun leaveChannel() {
        agora.leaveChannel()
    }

    companion object {
        private const val TAG = "AgVoiceRecognition"
        private fun launchInMainScope(block: suspend () -> Unit) {
            CoroutineScope(Dispatchers.Main).launch {
                block()
            }
        }
    }
}