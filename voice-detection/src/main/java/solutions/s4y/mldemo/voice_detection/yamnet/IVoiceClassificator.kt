package solutions.s4y.mldemo.voice_detection.yamnet

import kotlinx.coroutines.flow.SharedFlow
import java.io.Closeable

interface IVoiceClassificator: Closeable {
    fun addSamples(samples: ShortArray)
    /**
     * Flow emits a pair of list of labels and corresponding wave forms (pcm normalized to -1..1)
     */
    val classifierFlow: SharedFlow<Pair<List<String>, FloatArray>>
}