package solutions.s4y.mldemo.voice_detection.yamnet

import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface IVoiceClassifier: Closeable {
    class Classes(val ids: List<Int>, val waveForms: FloatArray)
    val flow: Flow<Classes>
    fun labels (classes: List<Int>): List<String>
    fun probabilities (classes: List<Int>): List<Float>
}