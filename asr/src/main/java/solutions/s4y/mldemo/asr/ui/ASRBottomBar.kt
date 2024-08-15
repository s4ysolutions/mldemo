package solutions.s4y.mldemo.asr.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.viewmodels.ASRViewModel

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ASRBottomBar(viewModel: ASRViewModel = viewModel(factory = ASRViewModel.Factory)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // val au = viewModel.audioService
    // val classifier = viewModel.classifier
    // val auStatus = au.recordingStatusFlow.collectAsState(initial = au.currentStatus)
    val permissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    BottomAppBar(
        actions = {
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    /*
                    if (permissionState.status.isGranted) {
                        if (auStatus.value == AudioService.RecordingStatus.RECORDING) {
                            au.stopRecording()
                            classifier.stop()
                        } else {
                            au.startRecording()
                            classifier.start(au.samplesFlow, scope)
                        }
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                     */
                },
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                if (permissionState.status.isGranted) {
                    /*
                    if (auStatus.value == AudioService.RecordingStatus.RECORDING)
                        Icon(Icons.Filled.StopCircle, "Stop recording")
                    else
                     */
                        Icon(Icons.Filled.PlayCircle, "Start recording")
                } else {
                    Icon(Icons.Filled.PlayDisabled, "Request permission")
                }
            }
        }
    )
}
