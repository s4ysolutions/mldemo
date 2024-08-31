package solutions.s4y.tflite

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class TfLiteStandaloneInterpreterTest {
    @Test
    fun interpreter_shouldRun_float2x3toInt3x3() = runTest {
        // Arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val factory = TfLiteStandaloneFactory()
        val interpreter = factory.createInterpreterFromAsset(
            appContext,
            "float2x3-to-int3x2.tflite",
            "asset-model"
        )
        // Act
        interpreter.run(floatArrayOf(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f))
        // Assert
        assertArrayEquals(intArrayOf(1, 4, 2, 5, 3, 6), interpreter.intOutput)
    }
}
