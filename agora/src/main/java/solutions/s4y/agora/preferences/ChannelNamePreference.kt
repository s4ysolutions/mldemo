package solutions.s4y.agora.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelNamePreference @Inject constructor(@ApplicationContext context: Context) :
    StringPreferenceStateFlow(
        MutableStateFlow(
            context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE)
                .getString("channel_name", "") ?: ""
        ),
        context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE),
        "channel_name"
    )