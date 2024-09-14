package solutions.s4y.agora.services.androidspeech

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import solutions.s4y.agora.preferences.LanguagePreference
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class AndroidRecognizer(
    private val context: Context,
    private val languagePreference: LanguagePreference,
    private val tag: String? = null,
    private val onRecognized: (String) -> Unit
) : Closeable {
    enum class State {
        NULL,
        CREATING,
        READY,
        WORKING,
        DESTROYING,
    }

    private val mutableStateFlow = MutableStateFlow(State.NULL)
    private val pcmBuffers: MutableList<ByteBuffer> = ArrayDeque()
    private val pcmUpdateMutex = Mutex()
    private var recognizerRef = AtomicReference<AndroidRecognizerWithStream?>(null)
    private val recognizerMutex = Mutex()
    private val timeoutStreamWriter = TimeoutStreamWriter(5000, tag)
    private val writeFailedCount = AtomicInteger(0)

    private val recognizerListener = AndroidRecognizerListener(
        onReady = {
            mutableStateFlow.value = State.READY
            if (tag != null) {
                Log.d(tag, "recognizerListener: onReady, try to flush")
            }
            launchInDefaultScope { flushPcmArrays() }
        },
        onError = { code, message ->
            if (tag != null) {
                Log.d(tag, "recognizerListener: onError($code): $message, will destroy")
            }
            launchInMainScope { destroyRecognizer() }
        },
        onPartial = { partial ->
            onRecognized(partial)
            mutableStateFlow.value = State.READY
            if (tag != null) {
                Log.d(tag, "recognizerListener: onPartial: try to flush")
            }
            launchInMainScope { flushPcmArrays() }
        },
        onResult = { result ->
            onRecognized(result)
            if (tag != null) {
                Log.d(tag, "recognizerListener: onResult: $result, will destroy")
            }
            launchInMainScope { destroyRecognizer() }
        },
        tag = tag
    )

    val stateFlow: StateFlow<State> = mutableStateFlow

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun addPcm(pcm: ByteBuffer) {
        // accumulate up to 10 seconds (assuming buffer is 16000 samples of 16kHz)
        pcmUpdateMutex.withLock {
            pcmBuffers.add(pcm)
            if (pcmBuffers.size > 5) {
                pcmBuffers.removeFirst()
            }
            if (tag != null) {
                Log.d(tag, "addPcm: ${pcmBuffers.size}")
            }
        }
        if (tag != null) {
            Log.d(tag, "check status after addPCM")
        }
        // now take a look if we can send it to recognizer
        when (mutableStateFlow.value) {
            State.READY -> {
                if (tag != null) {
                    Log.d(tag, "addPcm: recognizer is ready, trying to flush")
                }
                // can write to recognizer
                flushPcmArrays()
            }

            State.CREATING,
            State.DESTROYING,
            State.WORKING -> {
                // recognizer is busy, do nothing
                if (tag != null) {
                    Log.d(tag, "addPcm: recognizer is busy: ${mutableStateFlow.value.toString()}")
                }
            }

            State.NULL -> {
                if (tag != null) {
                    Log.d(tag, "addPcm: recognizer is null, creating")
                }
                createRecognizer()
            }
        }
    }

    override fun close() {
        if (tag != null) {
            Log.d(tag, "close")
        }
        launchInDefaultScope { destroyRecognizer() }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun createRecognizer() = recognizerMutex.withLock {
        val recognizer = recognizerRef.get()
        assert(recognizer == null)
        assert(mutableStateFlow.compareAndSet(State.NULL, State.CREATING))
        writeFailedCount.set(0)
        if (tag != null) {
            Log.d(tag, "createRecognizer")
        }
        recognizerRef.set(AndroidRecognizerWithStream(context, tag, recognizerListener).apply {
            if (tag != null) {
                Log.d(tag, "startListening")
            }
            startListening(languagePreference.value)
        })
    }

    private suspend fun destroyRecognizer() = recognizerMutex.withLock {
        if (tag != null) {
            Log.d(tag, "destroyRecognizer: enter")
        }
        recognizerRef.get()?.let {
            it.close()
            if (tag != null) {
                Log.d(tag, "destroyRecognizer: recognizer closed")
            }
            mutableStateFlow.value = State.NULL
            recognizerRef.set(null)
        } ?: run {
            if (tag != null) {
                Log.d(tag, "destroyRecognizer: skip, recognizer is null")
            }
        }
    }

    private suspend fun flushPcmArrays() {
        assert(mutableStateFlow.compareAndSet(State.READY, State.WORKING))
        // TODO: pass scope from outside?
        // TODO: error handler?
        val bytesArray = pcmUpdateMutex.withLock {
            val totalSize = pcmBuffers.sumOf { it.remaining() }
            if (totalSize == 0)
                return@withLock ByteArray(0)
            val bytesArray = ByteArray(totalSize)
            var offset = 0
            for (pcm in pcmBuffers) {
                val size = pcm.remaining()
                pcm.get(bytesArray, offset, size)
                offset += size
            }
            pcmBuffers.clear()
            bytesArray
        }
        if (tag != null) {
            Log.d(tag, "flushPcmArrays: ${bytesArray.size}")
        }
        if (bytesArray.isEmpty()) {
            mutableStateFlow.value = State.READY
            return
        }
        recognizerMutex.withLock {
            val recognizer = recognizerRef.get()
            if (recognizer != null) {
                if (tag != null) {
                    Log.d(tag, "flushPcmArrays: writeToStreamWithTimeout ...")
                }
                val result = withContext(Dispatchers.IO) {
                    timeoutStreamWriter.writeToStreamWithTimeout(
                        recognizer.writeStream,
                        bytesArray
                    )
                }
                if (tag != null) {
                    Log.d(tag, "flushPcmArrays: writeToStreamWithTimeout result: $result")
                }
                if (result < 0) {
                    writeFailedCount.incrementAndGet()
                } else {
                    writeFailedCount.set(0)
                }
            }
        }
        if (tag != null) {
            Log.d(tag, "check writeFailedCount: ${writeFailedCount.get()}")
        }
        if (writeFailedCount.get() > 1) {
            if (tag != null) {
                Log.e(tag, "flushPcmArrays: too many write failures, destroying recognizer")
            }
            destroyRecognizer()
        } else {
            mutableStateFlow.value = State.READY
        }
    }

    companion object {
        // TODO: error handler
        private fun launchInDefaultScope(block: suspend () -> Unit) {
            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch(Dispatchers.Default) {
                block()
            }
        }

        private fun launchInMainScope(block: suspend () -> Unit) {
            val scope = CoroutineScope(Dispatchers.Main)
            scope.launch(Dispatchers.Main) {
                block()
            }
        }
    }
}