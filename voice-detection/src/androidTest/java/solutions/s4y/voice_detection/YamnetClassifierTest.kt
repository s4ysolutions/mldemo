package solutions.s4y.voice_detection

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.runner.RunWith
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassificator

import solutions.s4y.mldemo.voice_detection.yamnet.YamnetClassifier
import solutions.s4y.audio.pcm.PCMAssetWavProvider

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
        val classifier = YamnetClassifier(appContext)
        val pcmProvider = PCMAssetWavProvider(appContext, "adam/1-1.wav")
        val pcm = pcmProvider.shorts
        // Act
        val results = mutableListOf<IVoiceClassificator.Labels>()
        val job = launch {
            classifier.labelsFlow.toList(results)
        }
        delay(10)

        val duration = 5 // seconds
        val start = 16000* duration
        val end = start + 16000*duration
        val st = 1024
        for (i in start until end step st) {
            val pcm200 = pcm.copyOfRange(i, i + st)
            classifier.addSamples(pcm200)
        }
        delay(10)
        classifier.close()
        job.join()
        // Assert
        assertEquals(duration, results.size)
        results.forEach {
            val labels = it.labels
            val label = labels[0].split("|")[1]
            assertEquals("Speech", label)
        }
    }
}