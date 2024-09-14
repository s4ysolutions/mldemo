package solutions.s4y.agora

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import solutions.s4y.agora.services.androidspeech.AndroidRecognizer
import solutions.s4y.audio.pcm.PCMAssetWavProvider
import java.nio.ByteBuffer
import java.nio.ByteOrder


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AndroidRecognizerTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET,
    )

    @Test
    fun addPcm_shouldRecognize() = runBlocking {
        // Arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        )
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            appContext.checkSelfPermission(Manifest.permission.INTERNET)
        )

        val provider = PCMAssetWavProvider(appContext, "OSR_us_000_0030_16k.wav")
        val length = 3
        val byteBuffer = ByteBuffer.allocate(16000 * length * 2) // 2 bytes per short
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        for (i in 16000 until 16000+16000*length) {
            byteBuffer.putShort(provider.shorts[i])
        }

        byteBuffer.flip()
        val cb: (String) -> Unit = mock()
        val recognizer = AndroidRecognizer(appContext, "AgVoiceRecognition") {
            cb(it)
        }
        // Act
        withContext(Dispatchers.Main) {
            recognizer.addPcm(byteBuffer)
        }
        // Assert
        verify(cb, timeout(10000)).invoke("paint the sockets")
    }
}