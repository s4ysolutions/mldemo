package solutions.s4y.pcm

import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test


class FlowTest {
    @Test
    fun customFlow_canBeClosed(): Unit = runBlocking {
        val f = callbackFlow<Int> {
            send(1)
            send(2)
            send(3)
            close()
        }
        val l = mutableListOf<Int>()
        f.toList(l)
        assertEquals(listOf(1, 2, 3), l)
    }

    class CbManager {
        private var cb: (Int) -> Unit = {}
        private var _close: () -> Unit = {}
        fun setCb(cb: (Int) -> Unit) {
            this.cb = cb
        }
        fun send(i: Int) {
            cb(i)
        }
        fun setClose(close: () -> Unit) {
            this._close = close
        }
        fun close() {
            _close()
        }
    }
    @Test
    fun customFlow_canBeClosedFromCallback(): Unit = runBlocking {
        val cbManager = CbManager()
        val f = callbackFlow<Int> {
            cbManager.setCb { trySend(it) }
            cbManager.setClose { close() }
            awaitClose()
        }
        val l = mutableListOf<Int>()
        val job = launch {
            f.toList(l)
        }

        delay(1)
        cbManager.send(1)
        delay(1)
        cbManager.send(2)
        delay(1)
        cbManager.send(3)
        delay(1)
        cbManager.close()

        assertEquals(listOf(1, 2, 3), l)
    }
}