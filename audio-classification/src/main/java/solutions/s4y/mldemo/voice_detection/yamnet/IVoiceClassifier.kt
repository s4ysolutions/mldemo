package solutions.s4y.mldemo.voice_detection.yamnet

import android.content.Context
import java.io.Closeable

interface IVoiceClassifier {
    class Classes(val ids: List<Int>, val waveForms: FloatArray) {
        val isSpeech: Boolean
            // Speech || Child speech, kid speaking || Narration, monologue || *Music* || Singing || TODO: chant
            get() = ids.isNotEmpty() && ids[0] in listOf(0, 1, 3, /*24,*/132)
    }

    val inputSize: Int
    val duration: Int
    suspend fun initialize(context: Context)
    suspend fun classify(waveForms: FloatArray): Classes
    fun labels(classes: List<Int>): List<String>
    fun probabilities(classes: List<Int>): List<Float>
}