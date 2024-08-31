package solutions.s4y.audio.batch

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

fun Flow<ShortArray>.batch(batchSize: Int): Flow<FloatArray> = flow{
    val accumulator = BatchAccumulator(batchSize)
    collect{
        accumulator.add(it)
        println("flow.batch: collected")
        var batch = accumulator.batch()
        while(batch != null){
            println("flow.batch: emit existing batch")
            emit(batch)
            yield()
            println("flow.batch: checking for next batch")
            batch = accumulator.batch()
        }
    }
}
