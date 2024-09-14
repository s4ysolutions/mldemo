package solutions.s4y.agora.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguagePreference @Inject constructor(@ApplicationContext context: Context) :
    StringPreferenceStateFlow(
        MutableStateFlow(
            context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE)
                .getString("language", "en") ?: "en"
        ),
        context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE),
        "language"
    )