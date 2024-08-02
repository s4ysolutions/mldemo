package solutions.s4y.mldemo.voice_transcription.obsolete.pytorch.tensors

import org.pytorch.Tensor
import solutions.s4y.pcm.IPCMProvider

class PCMTensorProvider(shortsArray: FloatArray): ITensorProvider {
    constructor(pcmProvider: IPCMProvider): this((pcmProvider.floats))
    override val tensor: Tensor by lazy {
        val floatsArray = FloatArray(shortsArray.size)
        val k = Short.MAX_VALUE.toFloat()
        for (i in shortsArray.indices) {
            floatsArray[i] = shortsArray[i] / k
        }
        Tensor.fromBlob(floatsArray, longArrayOf(1, floatsArray.size.toLong()))
    }
}