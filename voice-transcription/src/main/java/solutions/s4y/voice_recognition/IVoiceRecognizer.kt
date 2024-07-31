package solutions.s4y.voice_recognition

interface IVoiceRecognizer {
    fun recognize(pcm: FloatArray): String
}