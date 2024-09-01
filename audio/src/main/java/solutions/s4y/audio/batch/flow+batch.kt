package solutions.s4y.audio.batch

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

fun Flow<ShortArray>.batch(batchSize: Int): Flow<FloatArray> = flow{
    val accumulator = BatchAccumulator(batchSize)
    collect{
        accumulator.add(it)
        var batch = accumulator.batch()
        while(batch != null){
            emit(batch)
            yield()
            batch = accumulator.batch()
        }
    }
}
