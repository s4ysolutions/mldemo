package solutions.s4y.mldemo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MLApplication: Application() {
    companion object {
        lateinit var instance: MLApplication
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}