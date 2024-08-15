package solutions.s4y.mldemo.voice_detection.yamnet

abstract class DecoderBase(private val labels: List<String>) : IDecoder {
    private var lastProbabilities: FloatArray = FloatArray(labels.size) { 0.0f }

    // memoized
    private var classesDescendedM: List<Int>? = null

    protected abstract fun processProbabilities(probabilities: FloatArray): FloatArray

    override fun add(probabilities: FloatArray) {
        lastProbabilities = processProbabilities(probabilities)
        classesDescendedM = null
    }

    override val classesDescended: List<Int>
        get() = classesDescendedM ?: run {
            val result =
                lastProbabilities.indices.sortedByDescending { lastProbabilities[it] }
            classesDescendedM = result
            result
        }

    override fun labels(classes: List<Int>): List<String> = classes.map { labels[it] }

    override fun probabilities(classes: List<Int>): List<Float> =
        classes.map { lastProbabilities[it] }
}