package solutions.s4y.mldemo.ui.composable.navigation

import solutions.s4y.mldemo.R

enum class Destinations(val route: String, val title: Int) {
    Agora("agora", R.string.agora),
    Guesser("guesser", R.string.guesser),
    ASR("voice_transcription", R.string.asr),
    VoiceClassification("voice_detection", R.string.audio_classification);

    companion object {
        val defaultRoute: Destinations = Agora
    }
}