package solutions.s4y.mldemo.hilt

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier
import solutions.s4y.mldemo.voice_detection.yamnet.YamnetVoiceClassifier
import solutions.s4y.tflite.base.TfLiteFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ModuleSingletons {
    @Provides
    @Singleton
    fun provideVoiceClassifier(tfLiteFactory: TfLiteFactory): IVoiceClassifier {
        return YamnetVoiceClassifier(tfLiteFactory)
    }
}