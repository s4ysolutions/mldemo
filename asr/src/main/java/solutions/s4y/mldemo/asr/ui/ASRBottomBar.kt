package solutions.s4y.mldemo.asr.ui

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.QuestionMark
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import solutions.s4y.mldemo.asr.service.AsrModels
import solutions.s4y.mldemo.asr.service.whisper.SpeechState
import solutions.s4y.mldemo.asr.viewmodels.ASRViewModel

private const val TAG = "ASRBottomBar"

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ASRBottomBar(viewModel: ASRViewModel = hiltViewModel()) {
    val context = LocalContext.current

    val asr = viewModel.asrService
    val classifier = asr.classifier

    val asrCurrentModel by asr.flowCurrentModel.collectAsState()
    val asrIsActive by asr.flowIsActive.collectAsState()
    val asrIsModelReady by asr.flowIsReady.collectAsState()
    val asrSpeechState by asr.flowSpeechState.collectAsState()

    val classifierIsReady by classifier.flowIsReady.collectAsState()

    val tfLiteIsInferencing by asr.flowTfLiteIsInferencing.collectAsState()

    val permissionRecord = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val permissionRecordIsGranted = permissionRecord.status.isGranted

    val showModelsMenu = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            classifier.initialize(context)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load classifier: ${e.message}", Toast.LENGTH_LONG)
                .show()
            Log.e(TAG, "Failed to load model", e)
        }
    }
    LaunchedEffect(asrCurrentModel) {
        try {
            asr.switchModel(context, asrCurrentModel)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load model: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Failed to load model", e)
        }
    }

    BottomAppBar(actions = {
        Row(
            modifier = Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically
        ) {
            // icon to show ASR state
            val asrColor = when (asrSpeechState) {
                SpeechState.IDLE -> Color.Gray
                SpeechState.NON_SPEECH -> Color.Black
                SpeechState.SPEECH -> Color.Green
            }
            Icon(Icons.Filled.Mic, tint = asrColor, contentDescription = "ASR state")
            // icon to show decoding state
            val isDecodingColor = if (tfLiteIsInferencing) Color.Red else Color.Transparent
            Icon(
                Icons.Filled.Agriculture,
                tint = isDecodingColor,
                contentDescription = "Decoding"
            )
            // icon to show model state
            if (asrIsModelReady && classifierIsReady) {
                Icon(Icons.Filled.Check, contentDescription = "Model ready")
                //Text(model.toString(), Modifier.padding(start = 8.dp))
            } else {
                Icon(Icons.Filled.Downloading, contentDescription = "Model is not ready yet")
                //Text("$model (load)")
            }
            // icon to choose model
            // Box {
            IconButton(enabled = !asrIsActive, onClick = {
                showModelsMenu.value = true
            }) {
                Icon(Icons.Filled.Dialpad, contentDescription = "Models")
            }
            DropdownMenu(
                expanded = showModelsMenu.value,
                onDismissRequest = {
                    showModelsMenu.value = false
                },
            ) {
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text(AsrModels.Sergenes.toString() + "/tiny (en)")
                }, leadingIcon = {
                    if (asrCurrentModel == AsrModels.Sergenes) Icon(
                        Icons.Outlined.Dialpad, contentDescription = null
                    )
                }, onClick = {
                    showModelsMenu.value = false
                    viewModel.asrSwitchModel(context, AsrModels.Sergenes)
                })
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("Huggingface openai/tiny (en)")
                }, leadingIcon = {
                    if (asrCurrentModel == AsrModels.HuggingfaceTinyEn) Icon(
                        Icons.Outlined.Dialpad, contentDescription = null
                    )
                }, onClick = {
                    showModelsMenu.value = false
                    viewModel.asrSwitchModel(context, AsrModels.HuggingfaceTinyEn)
                })
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("Huggingface openai/base (en)")
                }, leadingIcon = {
                    if (asrCurrentModel == AsrModels.HuggingfaceBaseEn) Icon(
                        Icons.Outlined.Dialpad, contentDescription = null
                    )
                }, onClick = {
                    showModelsMenu.value = false
                    viewModel.asrSwitchModel(context, AsrModels.HuggingfaceBaseEn)
                })
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("Huggingface openai/tiny (ar)")
                }, leadingIcon = {
                    if (asrCurrentModel == AsrModels.HuggingfaceTinyAr) Icon(
                        Icons.Outlined.Dialpad, contentDescription = null
                    )
                }, onClick = {
                    showModelsMenu.value = false
                    viewModel.asrSwitchModel(context, AsrModels.HuggingfaceTinyAr)
                })
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("Huggingface openai/base (ar)")
                }, leadingIcon = {
                    if (asrCurrentModel == AsrModels.HuggingfaceBaseAr) Icon(
                        Icons.Outlined.Dialpad, contentDescription = null
                    )
                }, onClick = {
                    showModelsMenu.value = false
                    viewModel.asrSwitchModel(context, AsrModels.HuggingfaceBaseAr)
                })
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("Android SpeechRecognizer free")
                }, leadingIcon = {
                    if (asrCurrentModel == AsrModels.AndroidFreeForm) Icon(
                        Icons.Outlined.Dialpad, contentDescription = null
                    )
                }, onClick = {
                    showModelsMenu.value = false
                    viewModel.asrSwitchModel(context, AsrModels.AndroidFreeForm)
                })
            }
        }
    }, floatingActionButton = {
        FloatingActionButton(
            onClick = {
                if (!permissionRecordIsGranted) {
                    permissionRecord.launchPermissionRequest()
                } else {
                    if (asrIsActive) {
                        viewModel.asrStop()
                    } else {
                        // tflite can be still decoding while ASR already stopped
                        if (tfLiteIsInferencing) return@FloatingActionButton
                        if (!asrIsModelReady) return@FloatingActionButton
                        if (!classifierIsReady) return@FloatingActionButton
                        viewModel.asrStart(context)
                    }
                }
            },
            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
        ) {
            if (!permissionRecordIsGranted)
                Icon(Icons.Filled.QuestionMark, "Request permission")
            else if (asrIsActive)
                Icon(Icons.Filled.StopCircle, "Stop recording")
            else if (
                !asrIsModelReady ||
                !classifierIsReady ||
                tfLiteIsInferencing
            )
                Icon(Icons.Filled.PlayDisabled, "Can't start ASR")
            else
                Icon(Icons.Filled.PlayCircle, "Start recording")

        }
    })
}
