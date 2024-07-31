package solutions.s4y.voice_detection

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import solutions.s4y.mldemo.voice_detection.yamnet.YamnetClassifier
import solutions.s4y.pcm.PCMAssetWavProvider

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class YamnetClassifierTest {
    @Test
    fun classifierFlow_shouldEmit() = runBlocking {
        // Arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val scope = CoroutineScope(this.coroutineContext + Dispatchers.Default)
        val classifier = YamnetClassifier(appContext, scope)
        val pcmProvider = PCMAssetWavProvider(appContext, "adam/1-1.wav")
        val pcm = pcmProvider.floats.map { it.toInt().toShort() }.toShortArray()
        // Act

        val job = classifier
            .classifierFlow
            .onEach { result ->
                println(result)
            }
            .launchIn(this)
        val start = 16000*5
        val end = start + 16000*2
        for (i in start until end step 1024) {
            val pcm200 = pcm.copyOfRange(i, i + 1024)
            classifier.addSamples(pcm200)
        }
        delay(10000)
        // Assert

        job.cancel()
    }
}