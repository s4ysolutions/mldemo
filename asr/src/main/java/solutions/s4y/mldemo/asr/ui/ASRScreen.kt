package solutions.s4y.mldemo.asr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.viewmodels.ASRViewModel

@Composable
fun VoiceTranscriptionScreen(viewModel: ASRViewModel = hiltViewModel()) {
    val durationClassifier = viewModel.classifier.flowDuration.collectAsState().value
    val last = viewModel.asrService.flow.collectAsState("").value
    val durationLogMel = viewModel.asrService.flowLogMelDuration.collectAsState().value
    val durationDecode = viewModel.asrService.flowEncoderDecoderDuration.collectAsState().value
    val recordDuration = viewModel.asrService.flowRecordDuration.collectAsState().value
    val decodingDuration = viewModel.asrService.flowDecodingQueuedDuration.collectAsState().value
    val isDecoding = viewModel.asrService.flowIsDecoding.collectAsState().value
    val model = viewModel.currentModel.flow.collectAsState().value
    val currentScope = rememberCoroutineScope()
    val busySeconds = viewModel.flowBusySeconds.collectAsState().value
    val recordingStatue = viewModel.audioService.recordingStatusFlow.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.trackTranscribing(currentScope)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    )
    {
        Text(
            text = last,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        )
        Column(modifier = Modifier.align(Alignment.End)) {
            Text(text = "Model: $model")
            if (recordingStatue == AudioService.RecordingStatus.RECORDING && recordDuration > 0) {
                Text(text = "Record to decode: $recordDuration s")
            }
            if (isDecoding) {
                Text(text = "Current decoding:")
                Text(text = " - length of record of $decodingDuration s")
                Text(text = " - duration: $busySeconds s")
            }
            if (durationClassifier > 0 || last.isNotEmpty())
                Text(text = "TF Lite timings:")
            if (durationClassifier > 0)
                Text(text = " - voice detection: $durationClassifier ms")
            if (last.isNotEmpty()) {
                Text(text = " - LogMel spectrogram: $durationLogMel ms")
                Text(text = " - decode: $durationDecode ms")
            }
        }
    }
}
