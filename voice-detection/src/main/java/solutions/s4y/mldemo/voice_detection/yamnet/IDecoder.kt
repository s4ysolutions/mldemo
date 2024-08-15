package solutions.s4y.mldemo.voice_detection.yamnet

interface IDecoder {
    /**
     * Add probabilities for the last frame
     * @param probabilities array of probabilities
     * effectively it is a map LabelId<Int> -> Probability<Float>
     */
    fun add(probabilities: FloatArray)

    /**
     * @return list of probabilities in descending order
     * effectively it is a LabelIds sorted by probabilities
     */
    val classesDescended: List<Int>
    fun labels (classes: List<Int>): List<String>
    fun probabilities (classes: List<Int>): List<Float>
}