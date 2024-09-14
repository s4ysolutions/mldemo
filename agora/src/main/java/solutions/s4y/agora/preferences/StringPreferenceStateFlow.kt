package solutions.s4y.agora.preferences

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

open class StringPreferenceStateFlow(
    private val mutableStateFlow: MutableStateFlow<String>,
    private val preferences: SharedPreferences,
    private val key: String
) : MutableStateFlow<String> by mutableStateFlow {
    override var value: String
        get() = mutableStateFlow.value
        set(value) {
            mutableStateFlow.value = value
            preferences.edit().putString(key, value).apply()
        }
}