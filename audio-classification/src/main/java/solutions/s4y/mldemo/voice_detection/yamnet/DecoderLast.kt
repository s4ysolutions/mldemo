package solutions.s4y.mldemo.voice_detection.yamnet

class DecoderLast(labels: List<String>) : DecoderBase(labels) {
    override fun processProbabilities(probabilities: FloatArray): FloatArray = probabilities
}