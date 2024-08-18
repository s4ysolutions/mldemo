package solutions.s4y.voice_detection

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.runner.RunWith
import solutions.s4y.audio.accumulator.PCMFeed
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier

import solutions.s4y.mldemo.voice_detection.yamnet.YamnetVoiceClassifier
import solutions.s4y.audio.pcm.PCMAssetWavProvider
import kotlin.math.min

@OptIn(DelicateCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class YamnetVoiceClassifierTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun classifierFlow_shouldEmit() = runBlocking {
        // Arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val pcmFeed = PCMFeed(YamnetVoiceClassifier.PCM_BATCH)
        val inferenceContext = newSingleThreadContext("YamnetVoiceClassifierTest")
        val classifier = YamnetVoiceClassifier(appContext, pcmFeed.flow, inferenceContext)
        val pcmProvider = PCMAssetWavProvider(appContext, "adam/1-1.wav")
        val pcm = pcmProvider.shorts
        // Act
        val results = mutableListOf<IVoiceClassifier.Classes>()
        var started = false;
        val job = CoroutineScope(Dispatchers.IO).launch {
            var i = 1
            classifier
                .flow
                .take(5)
                .onStart {
                    started = true
                }
                .onEach {
                    Log.d(TAG, "onEach $i")
                    i++
                }
                .toList(results)
        }

        while (!started) {
            delay(20)
        }

        val duration = 5 // seconds
        val start = 16000 * duration
        val end = start + 16000 * duration
        val st = 10240
        for (i in start until end step st) {
            val pcm200 = pcm.copyOfRange(i, min(i + st, pcm.size))
            pcmFeed.add(pcm200)
            delay(5)
        }
        job.join()
        // Assert
        results.forEach {
            val labels = classifier.labels(it.ids)
            val label = labels[0]
            assertEquals("Speech", label)
        }
        assertEquals(duration, results.size)
        val f = MutableSharedFlow<Int>()
        withTimeout(1000) {
            f.emit(1)
        }
    }

    companion object {
        val f = MutableSharedFlow<Int>().subscriptionCount
        private val TAG = YamnetVoiceClassifierTest::class.java.simpleName
    }
}