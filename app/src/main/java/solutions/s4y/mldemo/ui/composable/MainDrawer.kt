package solutions.s4y.mldemo.ui.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import solutions.s4y.mldemo.R
import solutions.s4y.mldemo.ui.composable.navigation.Destinations

@Composable
fun MainDrawer(
    route: String,
    modifier: Modifier = Modifier,
    navigateToGuesser: () -> Unit,
    navigateToVoiceDetection: () -> Unit,
    navigateToVoiceTranscription: () -> Unit,
    closeDrawer: () -> Unit
) {
    ModalDrawerSheet(modifier = Modifier) {
        DrawerHeader(modifier)
        Spacer(modifier = Modifier.padding(5.dp))
        NavigationDrawerItem(
            label = {
                Text(
                    text = stringResource(id = Destinations.Guesser.title),
                    style = MaterialTheme.typography.labelSmall
                )
            },
            selected = route == Destinations.Guesser.route,
            onClick = {
                navigateToGuesser()
                closeDrawer()
            },
            icon = { Icon(imageVector = Icons.Default.RepeatOne, contentDescription = null) },
            shape = MaterialTheme.shapes.small
        )

        NavigationDrawerItem(
            label = { Text(text = stringResource(id = Destinations.VoiceDetection.title), style = MaterialTheme.typography.labelSmall) },
            selected = route == Destinations.VoiceDetection.route,
            onClick = {
                navigateToVoiceDetection()
                closeDrawer()
            },
            icon = { Icon(imageVector = Icons.Default.Mic, contentDescription = null) },
            shape = MaterialTheme.shapes.small
        )

        NavigationDrawerItem(
            label = { Text(text = stringResource(id = Destinations.VoiceTranscription.title), style = MaterialTheme.typography.labelSmall) },
            selected = route == Destinations.VoiceTranscription.route,
            onClick = {
                navigateToVoiceTranscription()
                closeDrawer()
            },
            icon = { Icon(imageVector = Icons.Default.Mic, contentDescription = null) },
            shape = MaterialTheme.shapes.small
        )
    }
}



@Composable
fun DrawerHeader(modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary)
            .padding(15.dp)
            .fillMaxWidth()
    ) {
        Image(imageVector = Icons.Default.Quiz, contentDescription = null)
        /*
        Image(
            painterResource(id = R.drawable.profile_picture),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(70.dp)
                .clip(CircleShape)
        )
         */
        Spacer(modifier = Modifier.padding(5.dp))

        Text(
            text = stringResource(id = R.string.app_name),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}