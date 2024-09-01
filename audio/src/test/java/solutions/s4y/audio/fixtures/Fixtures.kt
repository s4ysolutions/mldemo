package solutions.s4y.mldemo.asr.whisper.tokenizer.fixtures

import solutions.s4y.audio.pcm.IPCMProvider
import solutions.s4y.audio.pcm.PCMResourceWavProvider

interface Fixtures {
    companion object {
        private val classLoader = javaClass.classLoader!!

        val wav1_1: IPCMProvider by lazy {
            PCMResourceWavProvider("adam/1-1.wav")
        }
    }

    val wav1_1: IPCMProvider get() = Fixtures.wav1_1
}
