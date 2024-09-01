package solutions.s4y.mldemo.voice_detection.yamnet

import android.content.Context
import java.io.Closeable

interface IVoiceClassifier {
    class Classes(val ids: List<Int>, val waveForms: FloatArray)
    val inputSize: Int
    val duration: Long
    suspend fun initialize(context: Context)
    suspend fun classify(waveForms: FloatArray): Classes
    fun labels (classes: List<Int>): List<String>
    fun probabilities (classes: List<Int>): List<Float>
}