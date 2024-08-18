package solutions.s4y.mldemo.kotlin.fixtures

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow

interface FlowExt {
    suspend fun <T> MutableSharedFlow<T>.waitSubscribers() {
        while (subscriptionCount.value == 0) {
            delay(5)
        }
    }
}