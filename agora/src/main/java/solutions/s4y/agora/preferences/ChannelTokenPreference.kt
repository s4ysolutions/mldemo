package solutions.s4y.agora.preferences

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelTokenPreference @Inject constructor(@ApplicationContext context: Context) :
    StringPreferenceStateFlow(
        MutableStateFlow(
            context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE)
                .getString("channel_token", "") ?: ""
        ),
        context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE),
        "channel_token"
    ) {
    init {
        context.getSharedPreferences("s4y.agora", Context.MODE_PRIVATE)
            .edit().putString(
                "channel_token",
                "007eJxTYGi6IbPgwZTAVVzf9EN+SXll2gY7iqhpBnjeiveY/uX42UkKDMYGxgZmJgYGZknmxiYGpikWJmYmFqkpJpamKZbGiUaWz2c/TWsIZGRYvZSXkZEBAkF8TobkjMS8vNQcQyMGBgCBWh/L"
            ).apply()
    }
}