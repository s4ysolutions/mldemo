package solutions.s4y.interaction.user

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ThinkOf(modifier: Modifier = Modifier) {
    Text(
        text = "Think of zero or one, press the button",
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}