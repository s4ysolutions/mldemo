package solutions.s4y.firebase

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

import org.junit.Test

import org.junit.Assert.*
import org.junit.Rule
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import solutions.s4y.firebase.rules.FirebaseRule
import java.io.File

@RunWith(Enclosed::class)
class FirebaseBlobTest {
    class MetadataTest {
        @get:Rule
        val firebaseRule = FirebaseRule()

        @Test
        fun metadata_shouldReturn_whenExist(): Unit = runBlocking {
            // Arrange
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val localFile = File(appContext.filesDir, "whisper101_normalizer.json")
            val blob = FirebaseBlob("ml/whisper-1.0.1/normalizer.json", localFile)

            // Act
            val metadata = blob.getMetaData()
            // Assert
            assertEquals("ml/whisper-1.0.1/normalizer.json", metadata.path)
        }
    }

    class GetTest {
        @get:Rule
        val firebaseRule = FirebaseRule()

        @Test
        fun get_shouldDownload_whenNotExist(): Unit = runBlocking {
            // Arrange
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val localFile = File(appContext.filesDir, "whisper101_normalizer.json")
            val blob = FirebaseBlob("ml/whisper-1.0.1/normalizer.json", localFile)

            if (localFile.exists()) {
                localFile.delete()
            }
            assertFalse(localFile.exists())
            // Act
            blob.flow.collect()
            // Assert
            assertTrue(localFile.exists())
            assertEquals(52666, localFile.length())
        }

        @Test
        fun get_shouldReturnLocal_whenExist(): Unit = runBlocking {
            // Arrange
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val localFile = File(appContext.filesDir, "whisper101_normalizer.json")
            val blob = FirebaseBlob("ml/whisper-1.0.1/normalizer.json", localFile)
            // make sure local file does not exist
            if (localFile.exists()) {
                localFile.delete()
            }
            assertFalse(localFile.exists())
            // make sure local file is downloaded (i.e. exist)
            assertNull(blob.isLocal)
            blob.flow.collect()
            assertTrue(localFile.exists())
            assertTrue(blob.isLocal == false)
            // Act
            blob.flow.collect()
            // Assert
            assertTrue(blob.isLocal == true)
            assertTrue(localFile.exists())
        }

        @Test
        fun get_shouldDownload_whenExistButObsolete(): Unit = runBlocking {
            // Arrange
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val localFile = File(appContext.filesDir, "whisper101_normalizer.json")
            val blob = FirebaseBlob("ml/whisper-1.0.1/normalizer.json", localFile)
            // make sure local file does not exist
            if (localFile.exists()) {
                localFile.delete()
            }
            assertFalse(localFile.exists())
            // make sure local file is downloaded (i.e. exist)
            assertNull(blob.isLocal)
            blob.flow.collect()
            assertTrue(localFile.exists())
            assertTrue(blob.isLocal == false)
            // make sure local file is obsolete
            assertNotEquals(0, localFile.lastModified())
            localFile.setLastModified(0)
            assertEquals(0, localFile.lastModified())
            // Act
            blob.flow.collect()
            // Assert
            assertTrue(blob.isLocal == false)
            assertTrue(localFile.exists())
            assertNotEquals(0, localFile.lastModified())
        }
    }
}