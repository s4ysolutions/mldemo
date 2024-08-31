package solutions.s4y.voice_detection

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith
import solutions.s4y.audio.batch.batch
import solutions.s4y.mldemo.voice_detection.yamnet.IVoiceClassifier

import solutions.s4y.mldemo.voice_detection.yamnet.YamnetVoiceClassifier
import solutions.s4y.audio.pcm.PCMAssetWavProvider
import solutions.s4y.tflite.TfLiteStandaloneFactory
import solutions.s4y.tflite.base.TfLiteFactory

@RunWith(AndroidJUnit4::class)
class YamnetVoiceClassifierTest {
    private lateinit var tfLiteFactory: TfLiteFactory

    @Before
    fun before() {
        tfLiteFactory = TfLiteStandaloneFactory()
    }

    @After
    fun after() {
        tfLiteFactory.close()
    }


    @Test
    fun classifierFlow_shouldEmit() = runBlocking {
        // Arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val classifier = YamnetVoiceClassifier(tfLiteFactory).also {
            it.initialize(appContext)
        }
        val pcmProvider = PCMAssetWavProvider(appContext, "adam/1-1.wav")
        val pcm = pcmProvider.shorts
        val pcmFeed = MutableSharedFlow<ShortArray>()//extraBufferCapacity = 10)
        // Act
        val results = mutableListOf<IVoiceClassifier.Classes>()
        val started = Mutex(true)
        val job = CoroutineScope(Dispatchers.Default).launch {
            pcmFeed.batch(classifier.inputSize)
                .take(5)
                .onStart {
                    started.unlock()
                }
                .map { wavesForm ->
                    classifier.classify(wavesForm)
                }
                .toList(results)
        }

        println("waiting for start..")
        started.lock()
        println("started with ${pcmFeed.subscriptionCount.value} subscribers, waiting for subscription..")
        while (pcmFeed.subscriptionCount.value == 0) {
            yield()
        }
        println("subscribed, feeding..")

        // emulate audio source
        val start = 16000 * 5 // start from 5 seconds, skipping 'chants' for simplicity
        val end = start + 16000 * 5 + 1 // end at 10 seconds
        val st = 10240 // send samples less than 1 second
        for (i in start..<end step st) {
            val pcm200 = pcm.copyOfRange(i, i + st)
            pcmFeed.emit(pcm200)
            println("emitted $i to ${i + st} sample ${(i + st - start) / 16000} sec")
            delay(5) //emulate delay between audio samples
        }
        job.join()
        // Assert
        // all samples were speech
        results.forEach {
            val labels = classifier.labels(it.ids)
            val label = labels[0]
            assertEquals("Speech", label)
        }
        // there were duration classes found
        assertEquals((end - start) / 16000, results.size)
    }
}