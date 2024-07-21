package solutions.s4y.pcm

class PCM16000Resampler(upstream: IPCMProvider, inputSampleRate: Int): IPCMProvider {
    override val floats: FloatArray by lazy {
        val input = upstream.floats
        when (inputSampleRate) {
            16000 -> {
                input
            }
            8000 -> {
                val resampled = FloatArray(input.size * 2)
                for (i in input.indices) {
                    resampled[i * 2] = input.get(i)
                    resampled[i * 2 + 1] = input.get(i)
                }
                resampled
            }
            else -> {
                throw IllegalArgumentException("Unsupported sample rate: $inputSampleRate")
            }
        }
    }
}