package solutions.s4y.tflite

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class TfLiteStandaloneFactoryTest {
    @Test
    fun factory_shouldCreate_fromContext() = runTest {
        try {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val factory = TfLiteStandaloneFactory()
            factory.createInterpreterFromAsset(
                appContext,
                "float2x3-to-int3x2.tflite",
                "asset-model"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            fail(e.message)
        }
    }
}
