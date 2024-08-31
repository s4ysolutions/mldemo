package solutions.s4y.tflite.base

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import solutions.s4y.firebase.FirebaseBlob
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

abstract class TfLiteFactory : Closeable {
    private suspend fun initializeIfNeeded(context: Context) = withContext(initializeContext) {
        if (isInitialized.getAndSet(true))
            return@withContext
        initialize(context)
    }

    protected abstract suspend fun initialize(context: Context)
    protected abstract suspend fun createInterpreter(
        context: Context,
        inferenceContext: CoroutineContext,
        modelBuffer: ByteBuffer,
        onClose: () -> Unit
    ): TfLiteInterpreter

    private suspend fun createInterpreter(
        context: Context,
        modelBuffer: ByteBuffer,
        name: String,
    ): TfLiteInterpreter {
        initializeIfNeeded(context)
        val threadFactory = ThreadFactory { runnable ->
            Thread(runnable, name)
        }
        // This single-threaded dispatched context need to run inference in the
        // same thread as interpreter was created
        // It must be created within the factory in order to make constructor
        // to run within and avoid parallel initialization
        // Anti-pattern: the context is created outside of the instance
        //               but must be freed by the instance. This is why
        //               onClose has to exist.
        val executor = Executors.newSingleThreadExecutor(threadFactory)
        val dispatcher = executor.asCoroutineDispatcher()
        val inferenceContext = dispatcher + errorHandler
        return withContext(inferenceContext) {
            inferrerThreadId = Thread.currentThread().id
            Log.d(TAG, "Create Interpreter in thread id: $inferrerThreadId")
            createInterpreter(context, inferenceContext, modelBuffer, onClose = {
                dispatcher.close()
                executor.shutdown()
            })
        }
    }

    suspend fun createInterpreterFromAsset(
        context: Context,
        assetName: String,
        name: String
    ): TfLiteInterpreter {
        val assetFileDescriptor = context.assets.openFd(assetName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val startOffset: Long = assetFileDescriptor.startOffset
        val declaredLength: Long = assetFileDescriptor.getDeclaredLength()
        val buffer = fileInputStream.mappedByteBuffer(startOffset, declaredLength)
        return createInterpreter(context, buffer, name)
    }

    suspend fun createInterpreterFromGCS(
        context: Context,
        gcsPath: String,
        name: String
    ): TfLiteInterpreter {
        val localFile = File(context.filesDir, gcsPath)
        Log.d(TAG, "Load model from GCS: $gcsPath")
        Log.d(TAG, "Load model to local path: ${localFile.absolutePath}")
        FirebaseBlob(gcsPath, localFile).get()
        val (fileInputStream, size) = withContext(Dispatchers.IO) {
            FileInputStream(localFile) to localFile.length()
        }
        val buffer = fileInputStream.mappedByteBuffer(0, size)
        return createInterpreter(context, buffer, name)
    }

    override fun close() {
    }

    protected companion object {
        private const val TAG = "TFLiteInterpreterFactory"
        private var inferrerThreadId: Long = -1
        private val isInitialized = AtomicBoolean(false)
        private val errorHandler = CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Coroutine error", e)
        }

        fun FileInputStream.mappedByteBuffer(
            startOffset: Long,
            declaredLength: Long
        ): MappedByteBuffer = use { inputStream ->
            inputStream.channel.use { fileChannel ->
                fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }

        // This single-threaded dispatched context need to avoid concurrent initialization
        private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        private val initializeContext = dispatcher + errorHandler

    }
}