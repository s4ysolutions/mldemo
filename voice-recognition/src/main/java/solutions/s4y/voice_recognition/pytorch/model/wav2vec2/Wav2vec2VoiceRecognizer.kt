package solutions.s4y.voice_recognition.pytorch.model.wav2vec2

import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import solutions.s4y.voice_recognition.IVoiceRecognizer
import solutions.s4y.voice_recognition.pytorch.tensors.ITensorProvider
import solutions.s4y.voice_recognition.pytorch.tensors.PCMTensorProvider

class Wav2vec2VoiceRecognizer(private val module: Module):IVoiceRecognizer {
    private fun recognize(value: IValue): String =
        module.forward(value).toStr()

    private fun recognize(tensor: Tensor): String =
        recognize(IValue.from(tensor))

    private fun recognize(tensorProvider: ITensorProvider): String =
        recognize(tensorProvider.tensor)

    override fun recognize(pcm: FloatArray): String = recognize(PCMTensorProvider(pcm))
}