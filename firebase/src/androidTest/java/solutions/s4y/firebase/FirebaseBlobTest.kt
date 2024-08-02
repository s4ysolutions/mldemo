package solutions.s4y.firebase

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule
import solutions.s4y.firebase.rules.FirebaseRule
import java.io.File

class FirebaseBlobTest {

    @get:Rule
    val firebaseTestRule = FirebaseRule()

    @Test
    fun get_shouldDownload_whenNotExist(): Unit = runBlocking{
        // Arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val localFile = File(appContext.filesDir, "whisper101_normalizer.json")
        val blob = FirebaseBlob("ml/whisper-1.0.1/normalizer.json", localFile)

        if (localFile.exists()) {
            localFile.delete()
        }
        assertFalse(localFile.exists())
        // Act
        blob.get().collect()
        // Assert
        assertTrue(localFile.exists())
        assertEquals(52666, localFile.length())
    }


    @Test
    fun get_shouldReturn_whenExist(): Unit = runBlocking{
        // Arrange
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val localFile = File(appContext.filesDir, "whisper101_normalizer.json")
        val blob = FirebaseBlob("ml/whisper-1.0.1/normalizer.json", localFile)

        if (localFile.exists()) {
            localFile.delete()
        }
        assertFalse(localFile.exists())
        // Act && Assert
        assertNull(blob.isLocal)
        blob.get().collect()
        assertTrue(localFile.exists())
        assertTrue(blob.isLocal == false)
        blob.get().collect()
        // Assert
        assertTrue(blob.isLocal == true)
        assertTrue(localFile.exists())
    }
}