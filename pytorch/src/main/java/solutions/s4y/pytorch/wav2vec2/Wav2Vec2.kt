package solutions.s4y.pytorch.wav2vec2

import android.content.Context
import org.pytorch.IValue
import org.pytorch.LitePyTorchAndroid
import org.pytorch.Tensor
import solutions.s4y.pytorch.ITensorProvider
import solutions.s4y.pytorch.PCMTensorProvider

class Wav2Vec2(context: Context) {
    private val module = LitePyTorchAndroid.loadModuleFromAsset(context.assets, "wav2vec2.ptl")

    @Suppress("MemberVisibilityCanBePrivate")
    fun recognize(value: IValue): String =
        module.forward(value).toStr()

    @Suppress("MemberVisibilityCanBePrivate")
    fun recognize(tensor: Tensor): String =
        recognize(IValue.from(tensor))

    fun recognize(tensorProvider: ITensorProvider): String =
        recognize(tensorProvider.tensor)

    @Suppress("unused")
    fun recognize(pcm: ShortArray): String = recognize(PCMTensorProvider(pcm))
}