package solutions.s4y.mldemo.asr.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.s4y.mldemo.asr.service.whisper.DecoderEncoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KProperty

@Singleton
class CurrentModel @Inject constructor(@ApplicationContext context: Context) :
    IAppProperty<DecoderEncoder.Models> {
    private val preferences = context.getSharedPreferences("s4y.asr", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(preferences.getInt(
        "current_model",
        DecoderEncoder.Models.HuggingfaceBaseEn.ordinal
    ).let { DecoderEncoder.Models.entries.toTypedArray()[it] })

    val flow: StateFlow<DecoderEncoder.Models> = _flow

    override var value: DecoderEncoder.Models
        get() {
            val model = preferences.getInt("current_model", 3)
            return DecoderEncoder.Models.entries.toTypedArray()[model]
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
        value: DecoderEncoder.Models
    ) {
        this.value = value
    }

    override operator fun invoke(): DecoderEncoder.Models = value
    override operator fun invoke(value: DecoderEncoder.Models) {
        this.value = value
    }
}