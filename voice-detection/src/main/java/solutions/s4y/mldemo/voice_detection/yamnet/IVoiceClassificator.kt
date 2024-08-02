package solutions.s4y.mldemo.voice_detection.yamnet

import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface IVoiceClassificator: Closeable {
    class Probabilities(val probabilities: FloatArray, val waveForms: FloatArray)
    class Labels(val labels: List<String>, val waveForms: FloatArray)
    fun addSamples(samples: ShortArray)
    val probabilitiesFlow: Flow<Probabilities>
    val labelsFlow: Flow<Labels>
}