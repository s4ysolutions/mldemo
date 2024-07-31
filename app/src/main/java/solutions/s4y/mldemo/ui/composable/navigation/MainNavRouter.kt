package solutions.s4y.mldemo.ui.composable.navigation

import androidx.navigation.NavHostController

class MainNavRouter(private val navController: NavHostController) {
    fun navigateToGuesser() {
        navController.navigate(Destinations.Guesser.route)
    }

    fun navigateToVoiceDetection() {
        navController.navigate(Destinations.VoiceDetection.route)
    }

    fun navigateToVoiceTranscription() {
        navController.navigate(Destinations.VoiceTranscription.route)
    }
}