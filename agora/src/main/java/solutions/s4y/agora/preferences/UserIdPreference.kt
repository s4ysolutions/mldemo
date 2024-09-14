package solutions.s4y.agora.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserIdPreference @Inject constructor(@ApplicationContext context: Context) :
    IntPreferenceStateFlow(
        MutableStateFlow(
            context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE)
                .getInt("user_id", 0) ?: 0
        ),
        context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE),
        "user_id"
    )