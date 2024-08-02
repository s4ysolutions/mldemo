package solutions.s4y.mldemo

import android.app.Application

class MLApplication: Application() {
    companion object {
        lateinit var instance: MLApplication
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}