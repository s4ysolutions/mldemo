package solutions.s4y.audio.pcm

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Paths

// TODO: deduplicate code
class PCMResourceWavProvider(resourcePath: String) : PCMMediaExtractorWavProvider(
    getMediaExtractor(resourcePath)
) {
    companion object {
        private val classLoader = javaClass.classLoader!!

        fun getMediaExtractor(resourcePath: String): MediaExtractor {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                throw UnsupportedOperationException("Requires API 26")
            val resource = classLoader.getResource(resourcePath)
            val tempFile = File.createTempFile("tempResource", null)
            tempFile.deleteOnExit()

            resource.openStream().use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            val extractor = MediaExtractor()
            extractor.setDataSource(tempFile.absolutePath)
            return extractor
        }
    }
}