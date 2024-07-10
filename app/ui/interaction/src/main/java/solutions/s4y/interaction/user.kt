package solutions.s4y.interaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import solutions.s4y.interaction.user.GuessButton
import solutions.s4y.interaction.user.ThinkOf

@Composable
fun UserInteraction(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        ThinkOf(modifier)
        Spacer(modifier = Modifier.height(16.dp))
        GuessButton(modifier)
    }
}
