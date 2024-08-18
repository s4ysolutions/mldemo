package solutions.s4y.mldemo.kotlin.fixtures

import org.junit.jupiter.api.BeforeEach

interface Reporting {
    fun println(msg: String) {
        kotlin.io.println("${System.currentTimeMillis() - ts0}:${System.currentTimeMillis() - ts}: $msg <- ${Thread.currentThread()}")
        ts = System.currentTimeMillis()
    }
    companion object {
        var ts: Long = 0
        var ts0: Long = 0
    }

    @BeforeEach
    fun resetTiming() {
        ts0 = System.currentTimeMillis()
        ts = ts0
    }
}