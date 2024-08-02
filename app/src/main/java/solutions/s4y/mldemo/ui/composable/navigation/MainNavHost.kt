package solutions.s4y.mldemo.ui.composable.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import solutions.s4y.mldemo.guesser.GuesserScreen
import solutions.s4y.mldemo.voice_detection.ui.VoiceDetectionScreen
import solutions.s4y.mldemo.voice_transcription.ui.VoiceTranscriptionScreen

@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = Destinations.VoiceDetection.route) {
        composable(Destinations.VoiceDetection.route) {
            VoiceDetectionScreen()
        }
        composable(Destinations.VoiceTranscription.route) {
            VoiceTranscriptionScreen()
        }
        composable(Destinations.Guesser.route) {
            GuesserScreen()
        }
    }
}