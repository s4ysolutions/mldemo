package solutions.s4y.mldemo.asr.service.whisper.extensions

import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

fun File.toByteBuffer(): ByteBuffer {
    FileInputStream(this).use { inputStream ->
        val fileChannel = inputStream.channel
        val startOffset = 0L
        val declaredLength = fileChannel.size()
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            .apply { order(ByteOrder.nativeOrder()) }
    }
}