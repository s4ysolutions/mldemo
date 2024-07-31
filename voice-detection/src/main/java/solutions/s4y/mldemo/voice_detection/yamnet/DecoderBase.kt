package solutions.s4y.mldemo.voice_detection.yamnet

abstract class DecoderBase(private val labels: List<String>): IDecoder {
    private var lastProbabilities: FloatArray = FloatArray(labels.size){0.0f}

    // memoized
    private var sortedProbabilitiesM: List<Int>? = null
    private var sortedLabelsM: List<String>? = null

    protected abstract fun processProbabilities(probabilities: FloatArray): FloatArray

    override fun add(probabilitiesDescended: FloatArray) {
        lastProbabilities = probabilitiesDescended
        sortedProbabilitiesM = null
        sortedLabelsM = null
    }

    override val labelsDescended: List<String>
        get() = sortedLabelsM ?: run {
            val result = probabilitiesIndicesDescended.map { "${lastProbabilities[it]}|${labels[it]}" }
            sortedLabelsM = result
            result
        }

    override val probabilitiesIndicesDescended: List<Int>
        get() = sortedProbabilitiesM ?: run {
            val result =
                lastProbabilities.indices.sortedByDescending { lastProbabilities[it] }
            sortedProbabilitiesM = result
            result
        }
}