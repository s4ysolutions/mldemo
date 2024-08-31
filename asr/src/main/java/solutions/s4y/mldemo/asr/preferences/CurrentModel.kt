package solutions.s4y.mldemo.asr.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.s4y.mldemo.asr.service.whisper.EncoderDecoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KProperty

@Singleton
class CurrentModel @Inject constructor(@ApplicationContext context: Context) :
    IAppProperty<EncoderDecoder.Models> {
    private val preferences = context.getSharedPreferences("s4y.asr", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(preferences.getInt(
        "current_model",
        EncoderDecoder.Models.HuggingfaceBaseEn.ordinal
    ).let { EncoderDecoder.Models.entries.toTypedArray()[it] })

    val flow: StateFlow<EncoderDecoder.Models> = _flow

    override var value: EncoderDecoder.Models
        get() {
            val model = preferences.getInt("current_model", 3)
            return EncoderDecoder.Models.entries.toTypedArray()[model]
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
        value: EncoderDecoder.Models
    ) {
        this.value = value
    }

    override operator fun invoke(): EncoderDecoder.Models = value
    override operator fun invoke(value: EncoderDecoder.Models) {
        this.value = value
    }
}