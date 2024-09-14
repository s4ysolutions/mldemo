package solutions.s4y.agora.preferences

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

open class IntPreferenceStateFlow(
    private val mutableStateFlow: MutableStateFlow<Int>,
    private val preferences: SharedPreferences,
    private val key: String
) : MutableStateFlow<Int> by mutableStateFlow {
    override var value: Int
        get() = mutableStateFlow.value
        set(value) {
            mutableStateFlow.value = value
            preferences.edit().putInt(key, value).apply()
        }
}