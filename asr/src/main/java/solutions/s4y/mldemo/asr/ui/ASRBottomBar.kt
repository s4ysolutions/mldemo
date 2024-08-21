package solutions.s4y.mldemo.asr.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.service.ASRService
import solutions.s4y.mldemo.asr.service.whisper.WhisperInferrer
import solutions.s4y.mldemo.asr.viewmodels.ASRViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ASRBottomBar() {
    val viewModel: ASRViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()

    val au = viewModel.audioService
    val auStatus = au.recordingStatusFlow.collectAsState(initial = au.currentStatus)

    val classifier = viewModel.classifier

    val asr = viewModel.asrService
    val asrVoice = asr.flowVoiceState.collectAsState()
    val modelReady = asr.flowModelReady.collectAsState()
    val isDecoding = asr.flowIsDecoding.collectAsState()

    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val model = WhisperInferrer.Models.Sergenes

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        asr.loadPipelineFromGCS(context, model)
    }

    BottomAppBar(
        actions = {
            Row {
                val asrState = asrVoice.value
                val asrColor = when (asrState) {
                    ASRService.State.IDLE -> Color.Gray
                    ASRService.State.NON_SPEECH -> Color.Black
                    ASRService.State.SPEECH -> Color.Green
                }
                Icon(Icons.Filled.Mic, tint = asrColor, contentDescription = "ASR state")

                val isDecodingColor =
                    if (isDecoding.value && asrState != ASRService.State.IDLE) Color.Red else Color.Transparent
                Icon(
                    Icons.Filled.Agriculture,
                    tint = isDecodingColor,
                    contentDescription = "Decoding"
                )

                if (modelReady.value) {
                    //Icon(Icons.Filled.Check, contentDescription = "Model ready")
                    Text(model.toString(), Modifier.padding(start = 8.dp))
                } else {
                    // Icon(Icons.Filled.Downloading, contentDescription = "Model is not ready yet")
                    Text("$model (load)")
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!modelReady.value) return@FloatingActionButton

                    if (permissionState.status.isGranted) {
                        if (auStatus.value == AudioService.RecordingStatus.RECORDING) {
                            au.stopRecording()
                            classifier.stop()
                            asr.stop()
                        } else {
                            au.startRecording()
                            classifier.start(au.samplesFlow, scope)
                            asr.start(classifier.flowClasses, scope)
                        }
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                },
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                if (permissionState.status.isGranted && modelReady.value) {
                    if (auStatus.value == AudioService.RecordingStatus.RECORDING)
                        Icon(Icons.Filled.StopCircle, "Stop recording")
                    else
                        Icon(Icons.Filled.PlayCircle, "Start recording")
                } else {
                    Icon(Icons.Filled.PlayDisabled, "Request permission")
                }
            }
        }
    )
}
