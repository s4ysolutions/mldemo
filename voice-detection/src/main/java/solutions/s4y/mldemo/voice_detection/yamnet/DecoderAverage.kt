package solutions.s4y.mldemo.voice_detection.yamnet

@Suppress("unused")
class DecoderAverage(labels: List<String>, private val n: Int = 3) : DecoderBase(labels) {
    private val accProbabilities = mutableListOf<FloatArray>()
    private val dim = labels.size

    override fun processProbabilities(probabilities: FloatArray): FloatArray {
        if (accProbabilities.size == n) {
            accProbabilities.removeAt(0)
        }
        accProbabilities.add(probabilities)
        val mean = FloatArray(dim) { 0.0f }
        val n = accProbabilities.size
        for (i in 0 until n) {
            for (j in 0 until dim) {
                mean[j] += accProbabilities[i][j]
            }
        }
        for (j in 0 until dim) {
            mean[j] = mean[j]/n
        }
        return mean
    }
}