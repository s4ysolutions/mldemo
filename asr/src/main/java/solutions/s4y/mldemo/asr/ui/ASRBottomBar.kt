package solutions.s4y.mldemo.asr.ui

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import solutions.s4y.mldemo.asr.service.whisper.DecoderEncoder
import solutions.s4y.mldemo.asr.viewmodels.ASRViewModel


private const val TAG = "ASRBottomBar"

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

    var currentModel by viewModel.currentModel

    val showModelsMenu = remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(currentModel) {
        try {
            // asr.loadPipelineFromGCS(context, currentModel)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load model: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Failed to load model", e)
        }
    }

    BottomAppBar(
        actions = {
            Row {
                // icon to show ASR state
                val asrState = asrVoice.value
                val asrColor = when (asrState) {
                    ASRService.State.IDLE -> Color.Gray
                    ASRService.State.NON_SPEECH -> Color.Black
                    ASRService.State.SPEECH -> Color.Green
                }
                Icon(Icons.Filled.Mic, tint = asrColor, contentDescription = "ASR state")
                // icon to show decoding state
                val isDecodingColor =
                    if (isDecoding.value && asrState != ASRService.State.IDLE) Color.Red else Color.Transparent
                Icon(
                    Icons.Filled.Agriculture,
                    tint = isDecodingColor,
                    contentDescription = "Decoding"
                )
                // icon to show model state
                if (modelReady.value) {
                    Icon(Icons.Filled.Check, contentDescription = "Model ready")
                    //Text(model.toString(), Modifier.padding(start = 8.dp))
                } else {
                    Icon(Icons.Filled.Downloading, contentDescription = "Model is not ready yet")
                    //Text("$model (load)")
                }
                // icon to choose model
                // Box {
                    IconButton(onClick = {
                        showModelsMenu.value = true
                    }) {
                        Icon(Icons.Filled.Dialpad, contentDescription = "Models")
                    }
                    DropdownMenu(
                        expanded = showModelsMenu.value,
                        onDismissRequest = { showModelsMenu.value = false },
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.padding(4.dp),
                            text = {
                                Text(DecoderEncoder.Models.Sergenes.toString() + " Tiny")
                            },
                            leadingIcon = {
                                if (currentModel == DecoderEncoder.Models.Sergenes)
                                    Icon(
                                        Icons.Outlined.Dialpad,
                                        contentDescription = null
                                    )
                            },
                            onClick = {
                                currentModel = DecoderEncoder.Models.Sergenes
                            })
                        DropdownMenuItem(
                            modifier = Modifier.padding(4.dp),
                            text = {
                                Text("HuggingFace OpenAI Base")
                            },
                            leadingIcon = {
                                if (currentModel == DecoderEncoder.Models.HuggingfaceBaseEn)
                                    Icon(
                                        Icons.Outlined.Dialpad,
                                        contentDescription = null
                                    )
                                Icon(
                                    Icons.Outlined.Dialpad,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                currentModel = DecoderEncoder.Models.HuggingfaceBaseEn
                            })
                    //}
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
