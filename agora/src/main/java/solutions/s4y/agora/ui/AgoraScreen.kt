package solutions.s4y.agora.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import solutions.s4y.agora.viewmodels.AgoraViewModel

@Composable
fun AgoraScreen(viewModel: AgoraViewModel = hiltViewModel()) {
    val transcription = viewModel.recognitionService.flowTranscriptions.collectAsState("").value
    val appId by viewModel.appIdPreference.collectAsState()
    val channelName by viewModel.channelNamePreference.collectAsState()
    val channelToken by viewModel.channelTokenPreference.collectAsState()
    val scrollState = rememberScrollState()

    Log.d("AgVoice", "transcription: $transcription")

    Column {
        Row(
            modifier = Modifier
                .padding(bottom = 2.dp, top = 2.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "App ID:", modifier = Modifier
                    .padding(end = 8.dp)
                    .wrapContentWidth()
            )
            TextField(
                value = appId,
                onValueChange = { viewModel.appIdPreference.value = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(0.dp)
                    .heightIn(20.dp),
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier
                .padding(bottom = 2.dp, top = 2.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Channel Name:", modifier = Modifier
                    .padding(end = 8.dp)
                    .wrapContentWidth()
            )
            TextField(
                value = channelName,
                onValueChange = { viewModel.channelNamePreference.value = it },
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
        Row(
            modifier = Modifier
                .padding(bottom = 2.dp, top = 2.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Channel Token:", modifier = Modifier
                    .padding(end = 8.dp)
                    .wrapContentWidth()
            )
            TextField(
                value = channelToken,
                onValueChange = { viewModel.channelTokenPreference.value = it },
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            item {
                Text(
                    text = transcription,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}