package solutions.s4y.mldemo.voice_detection.ui

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextAlign
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import solutions.s4y.mldemo.voice_detection.viewmodels.VoiceDetectionViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VoiceDetectionScreen() {
    val viewModel: VoiceDetectionViewModel = remember { VoiceDetectionViewModel() }
    val audio = viewModel.audioService
    val classifier = viewModel.voiceClassificationService
    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val samplesCount = audio.samplesCountFlow.collectAsState(initial = audio.samplesCount)

    val classes = classifier.classifierFlow.collectAsState(initial = listOf("No classes yet"))

    if (permissionState.status.isGranted) {
        Column {
            Text(
                text = "It is a Voice Detector",
                textAlign = TextAlign.Center
            )
            Text(
                text = samplesCount.value.toString(),
                textAlign = TextAlign.Center
            )
            classes.value.take(6).forEachIndexed { _, className ->
                Text(
                    text = className,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        val textToShow = if (permissionState.status.shouldShowRationale) {
            "The audio recording is important for this app. Please grant the permission."
        } else {
            "Camera permission required for this feature to be available. " +
                    "Please grant the permission"
        }
        Text(textToShow)
        Button(onClick = { permissionState.launchPermissionRequest() }) {
            Text("Request permission")
        }
    }

}
