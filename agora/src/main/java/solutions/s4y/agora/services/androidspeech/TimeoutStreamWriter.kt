package solutions.s4y.agora.services.androidspeech

import android.util.Log
import org.jetbrains.annotations.Blocking
import java.io.Closeable
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class TimeoutStreamWriter(private val timeoutMillis: Long, private val tag: String? = null) :
    Closeable {
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Blocking
    fun writeToStreamWithTimeout(
        outputStream: OutputStream,
        buffer: ByteArray,
    ): Int {
        val future: Future<Int>

        try {
            // Submit the write operation to run in a separate thread
            future = executor.submit<Int> {
                try {
                    if (tag != null) Log.d(tag, "Writing to android stream begins")
                    outputStream.write(buffer)
                    return@submit buffer.size
                } catch (e: Exception) {
                    throw RuntimeException("Error while writing to android stream", e)
                }
            }
            // Wait for the write operation to complete or timeout
            return future.get(
                timeoutMillis,
                TimeUnit.MILLISECONDS
            ) // returns the number of bytes written if successful
        } catch (e: TimeoutException) {
            if (tag != null)
                Log.e(tag, "Write to android stream timed out after $timeoutMillis milliseconds.")
        } catch (e: InterruptedException) {
            if (tag != null)
                Log.e(tag, "Write to android interrupted", e)
        }
        return -1
    }

    override fun close() {
        executor.shutdownNow()
    }
}