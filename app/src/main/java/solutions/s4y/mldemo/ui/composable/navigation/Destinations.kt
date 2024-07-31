package solutions.s4y.mldemo.ui.composable.navigation

import solutions.s4y.mldemo.R

enum class Destinations(val route: String, val title: Int) {
    Guesser("guesser", R.string.guesser),
    VoiceTranscription("voice_transcription", R.string.voice_transcription),
    VoiceDetection("voice_detection", R.string.voice_detection)
}