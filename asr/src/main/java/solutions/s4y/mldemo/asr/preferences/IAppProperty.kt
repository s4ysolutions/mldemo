package solutions.s4y.mldemo.asr.preferences

import kotlin.reflect.KProperty

interface IAppProperty<T> {
    var value: T
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
    operator fun invoke(): T
    operator fun invoke(value: T)
}