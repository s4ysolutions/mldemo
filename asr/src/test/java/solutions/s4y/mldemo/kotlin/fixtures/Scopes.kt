package solutions.s4y.mldemo.kotlin.fixtures

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext

interface Scopes {
    val computeScope get() = Companion.computeScope
    val processScope get() = Companion.processScope
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    companion object {
        val computeScope = CoroutineScope(newSingleThreadContext("compute"))
        val processScope = CoroutineScope(newFixedThreadPoolContext(10, "process"))
    }
}