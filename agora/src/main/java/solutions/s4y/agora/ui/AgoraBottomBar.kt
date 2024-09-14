package solutions.s4y.agora.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import solutions.s4y.agora.services.agora.AgoraPCMProvider
import solutions.s4y.agora.viewmodels.AgoraViewModel


@Composable
fun AgoraBottomBar(viewModel: AgoraViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val service = viewModel.recognitionService
    val agoraState = service.flowAngoraChannelState.collectAsState().value
    val showLanguagesMenu = remember { mutableStateOf(false) }
    val languagePreference = service.languagePreference

    BottomAppBar(actions = {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                showLanguagesMenu.value = true
            }) {
                Icon(Icons.Filled.Language, contentDescription = "Language")
            }
            DropdownMenu(
                expanded = showLanguagesMenu.value,
                onDismissRequest = {
                    showLanguagesMenu.value = false
                },
            ) {
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("Русский")
                }, onClick = {
                    showLanguagesMenu.value = false
                    languagePreference.value = "ru"
                })
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("Серпски")
                }, onClick = {
                    showLanguagesMenu.value = false
                    languagePreference.value = "sr"
                })
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("English")
                }, onClick = {
                    showLanguagesMenu.value = false
                    languagePreference.value = "en"
                })
                DropdownMenuItem(modifier = Modifier.padding(4.dp), text = {
                    Text("Arabic")
                }, onClick = {
                    showLanguagesMenu.value = false
                    languagePreference.value = "ar"
                })
            }
            IconButton(
                onClick = {
                    if (agoraState == AgoraPCMProvider.notJoined) {
                        service.joinChannel(context)
                    } else {
                        service.leaveChannel()
                    }
                },
            ) {
                if (agoraState == AgoraPCMProvider.notJoined)
                    Icon(Icons.Filled.Link, "Join channel")
                else
                    Icon(Icons.Filled.LinkOff, "Leave channel")

            }
        }
    })
}
