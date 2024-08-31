package solutions.s4y.tflite

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
@RunWith(AndroidJUnit4::class)
class TfLitePlayServicesFactoryTest {
    @Test
    fun factory_shouldCreate_fromContext() = runTest {
        try {
            // Context of the app under test.
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            assertEquals("solutions.s4y.tflite.test", appContext.packageName)
            val factory = TfLitePlayServicesFactory()
            factory.createInterpreterFromContext(
                appContext,
                "features-extractor.tflite",
                "asset-model"
            )
        }catch (e: Exception){
            e.printStackTrace()
            fail(e.message)
        }
    }
}
 */
