package solutions.s4y.mldemo.asr.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.s4y.mldemo.asr.service.AsrModels
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KProperty

@Singleton
class CurrentModel @Inject constructor(@ApplicationContext context: Context) :
    IAppProperty<AsrModels> {
    private val preferences = context.getSharedPreferences("s4y.asr", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(preferences.getInt(
        "current_model",
        AsrModels.AndroidFreeForm.ordinal
    ).let { AsrModels.entries.toTypedArray()[it] })

    val flow: StateFlow<AsrModels> = _flow

    override var value: AsrModels
        get() {
            val model = preferences.getInt("current_model", 3)
            return AsrModels.entries.toTypedArray()[model]
        }
        set(value) {
            preferences
                .edit()
                .putInt("current_model", value.ordinal)
                .apply()
            _flow.value = value
        }

    override operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        value

    override operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: AsrModels
    ) {
        this.value = value
    }

    override operator fun invoke(): AsrModels = value
    override operator fun invoke(value: AsrModels) {
        this.value = value
    }
}