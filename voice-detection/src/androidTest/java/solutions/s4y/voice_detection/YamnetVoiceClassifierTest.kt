package solutions.s4y.voice_detection

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.runner.RunWith
import solutions.s4y.audio.accumulator.PCMFeed
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier

import solutions.s4y.mldemo.voice_detection.yamnet.YamnetVoiceClassifier
import solutions.s4y.audio.pcm.PCMAssetWavProvider

@RunWith(AndroidJUnit4::class)
class YamnetVoiceClassifierTest {
    @Test
    fun classifierFlow_shouldEmit() = runBlocking {
        // Arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val pcmFeed = PCMFeed(YamnetVoiceClassifier.PCM_BATCH)
        val sharedFlowScope = CoroutineScope(Dispatchers.IO)
        val classifier = YamnetVoiceClassifier(appContext, pcmFeed, sharedFlowScope)
        val pcmProvider = PCMAssetWavProvider(appContext, "adam/1-1.wav")
        val pcm = pcmProvider.shorts
        // Act
        val results = mutableListOf<IVoiceClassifier.Classes>()
        val job = launch {
            withTimeout(1000) {
                classifier.flow.toList(results)
            }
        }
        delay(10)

        val duration = 5 // seconds
        val start = 16000* duration
        val end = start + 16000*duration
        val st = 1024
        for (i in start until end step st) {
            val pcm200 = pcm.copyOfRange(i, i + st)
            pcmFeed.add(pcm200)
            delay(10)
        }
        job.join()
        // Assert
        assertEquals(duration, results.size)
        results.forEach {
            val labels = classifier.labels(it.ids)
            val label = labels[0]
            assertEquals("Speech", label)
        }
    }
}