package solutions.s4y.mldemo.kotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.reflect.KProperty

class ValueTest {
    class SUT {
        var value = 10

        operator fun getValue(thisRef: Any?, property: KProperty<*>) = value

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            this.value = value
        }

        operator fun invoke() = value

        operator fun invoke(value: Int) {
            this.value = value
        }
    }

    @Test
    fun by_shouldAccess() {
        var sut by SUT()

        assertEquals(10, sut)
        sut = 11
        assertEquals(11, sut)
    }

    @Test
    fun invoke_shouldAccess() {
        val sut = SUT()

        assertEquals(10, sut())
        sut(11)
        assertEquals(11, sut())
    }
}