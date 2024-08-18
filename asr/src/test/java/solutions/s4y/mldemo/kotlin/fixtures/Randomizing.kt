package solutions.s4y.mldemo.kotlin.fixtures

import org.junit.jupiter.api.BeforeEach
import kotlin.random.Random

interface Randomizing {
    val random: Double get () = r.nextDouble()
    companion object {
        var r = Random(100)
    }

    @BeforeEach
    fun resetRandom() {
        r = Random(100)
    }
}