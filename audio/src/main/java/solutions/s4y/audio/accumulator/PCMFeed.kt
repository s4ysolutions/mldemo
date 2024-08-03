package solutions.s4y.audio.accumulator

class PCMFeed(
    batch: Int = 16000,
    private val accumulator: IWaveFormsAccumulator<Short, ShortArray> =
        MemoryWaveFormsAccumulator(batch)
) : IWaveFormsAccumulator<Short, ShortArray> by accumulator

