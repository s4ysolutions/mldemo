package solutions.s4y.mldemo.kotlin.fixtures

import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

interface LongTasks {
    fun longTask(): Double {
        val random = Random(System.currentTimeMillis())
        var x = 0.0
        for(i in 1..100000000) {
            val r = random.nextFloat()*exp(1.5)
            x += ln(r)
            if (x>1000) {
                x = 0.0
            }else if (x<-1000) {
                x = 0.0
            }
        }
        return x
    }
}