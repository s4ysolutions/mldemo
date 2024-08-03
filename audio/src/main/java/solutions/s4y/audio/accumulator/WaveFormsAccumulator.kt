package solutions.s4y.audio.accumulator

class WaveFormsAccumulator(
    batch: Int = 16000,
    private val accumulator: IWaveFormsAccumulator<Float, FloatArray> =
        MemoryWaveFormsAccumulator(batch)
) : IWaveFormsAccumulator<Float, FloatArray> by accumulator
