package solutions.s4y.mldemo.voice_detection.yamnet

interface IDecoder {
    fun add(probabilitiesDescended: FloatArray)
    val probabilitiesIndicesDescended: List<Int>
    val labelsDescended: List<String>
}