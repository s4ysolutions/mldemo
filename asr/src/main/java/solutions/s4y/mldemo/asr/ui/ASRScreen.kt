package solutions.s4y.mldemo.asr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import solutions.s4y.audio.AudioService
import solutions.s4y.mldemo.asr.viewmodels.ASRViewModel

@Composable
fun VoiceTranscriptionScreen(viewModel: ASRViewModel = hiltViewModel()) {
    val currentScope = rememberCoroutineScope()
    val asrService = viewModel.asrService

    val asrCurrentModel by asrService.flowCurrentModel.collectAsState()
    val asrLastResult by asrService.flowASR.collectAsState("")

    val classifierLastInferenceDuration by asrService.classifier.flowLastDuration.collectAsState()

    val recorderState by asrService.audioService.recordingStatusFlow.collectAsState()

    val tfLiteIsInferencing by asrService.flowTfLiteIsInferencing.collectAsState()
    val tfLiteInferencingDuration by asrService.flowTfLiteCurrentInferencingDuration.collectAsState()
    val tfLiteLastEncoderDecoderDuration by asrService.flowTfLiteLastEncoderDecoderDuration.collectAsState()
    val tfLiteLastLogMelDuration by asrService.flowTfLiteLastLogMelDuration.collectAsState()
    val tfLiteLastRecordSize by asrService.flowTfLiteLastRecordSize.collectAsState()
    val tfLiteAccumulatorSize by asrService.flowTfLiteAccumulatorSize.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(4.dp)
    ) {
        Text(
            text = asrLastResult,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        )
        Column(modifier = Modifier.align(Alignment.End)) {
            Text(text = "Model: $asrCurrentModel")
            if (recorderState == AudioService.RecordingStatus.RECORDING && tfLiteAccumulatorSize > 0) {
                Text(text = "Record to decode: $tfLiteAccumulatorSize s")
            }
            if (tfLiteIsInferencing) {
                Text(text = "Current decoding:")
                Text(text = " - length of record $tfLiteLastRecordSize s")
                Text(text = " - duration: $tfLiteInferencingDuration s")
            }
            if (classifierLastInferenceDuration > 0 || asrLastResult.isNotEmpty()) Text(text = "TF Lite timings:")
            if (classifierLastInferenceDuration > 0) Text(text = " - voice detection: $classifierLastInferenceDuration ms")
            if (tfLiteLastLogMelDuration > 0)
                Text(text = " - LogMel spectrogram: $tfLiteLastLogMelDuration ms")
            if (tfLiteLastEncoderDecoderDuration > 0) {
                Text(text = " - decode: $tfLiteLastEncoderDecoderDuration ms")
            }
        }
    }
}
