package solutions.s4y.mldemo.asr.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import solutions.s4y.mldemo.asr.viewmodels.ASRViewModel

@Composable
fun VoiceTranscriptionScreen(viewModel: ASRViewModel = hiltViewModel()) {
    val last = viewModel.asrService.flow.collectAsState("Listen ...").value
    Text(
        text = last,
        fontSize = 16.sp,
        modifier = Modifier.fillMaxSize().padding(4.dp),
    )
}
