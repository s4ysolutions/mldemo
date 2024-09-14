package solutions.s4y.agora.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIdPreference @Inject constructor(@ApplicationContext context: Context) :
    StringPreferenceStateFlow(
        MutableStateFlow(
            context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE)
                .getString("app_id", "") ?: ""
        ),
        context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE),
        "app_id"
    )