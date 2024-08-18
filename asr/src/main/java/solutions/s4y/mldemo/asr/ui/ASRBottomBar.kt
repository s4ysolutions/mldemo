package solutions.s4y.mldemo.asr.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Downloading
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.service.ASRService
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
    val asrStatus = asr.flowState.collectAsState()
    val modelReady = asr.flowModelReady.collectAsState()

    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    val model = "whisper-base.ar"

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        asr.loadModel(context, "ml/whisper-2.0.0/$model/whisper.tflite")
    }

    BottomAppBar(
        actions = {
            Row {
                val asrState = asrStatus.value
                val asrColor = when (asrState) {
                    ASRService.State.IDLE -> Color.Gray
                    ASRService.State.NON_SPEECH -> Color.Green
                    ASRService.State.SPEECH -> Color.Red
                }
                Icon(Icons.Filled.Circle, tint = asrColor, contentDescription = "ASR state")
                if (modelReady.value) {
                    //Icon(Icons.Filled.Check, contentDescription = "Model ready")
                    Text(model)
                }else {
                    // Icon(Icons.Filled.Downloading, contentDescription = "Model is not ready yet")
                    Text("$model (loading)")
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
