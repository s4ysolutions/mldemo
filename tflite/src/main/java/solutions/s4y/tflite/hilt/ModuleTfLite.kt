package solutions.s4y.mldemo.hilt

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import solutions.s4y.tflite.TfLiteStandaloneFactory
import solutions.s4y.tflite.base.TfLiteFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ModuleTfLite {
    @Provides
    @Singleton
    fun provideTfLiteFactory(): TfLiteFactory {
        return TfLiteStandaloneFactory()
    }
}