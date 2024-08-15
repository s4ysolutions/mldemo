package solutions.s4y.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioService @Inject constructor() {
    enum class RecordingStatus {
        IDLE,
        RECORDING,
    }

    private var audioRecord: AudioRecord? = null
    private val samplesCountAtomic = AtomicLong()
    private val recordingStatusMutable = MutableSharedFlow<RecordingStatus>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val samplesFlowMutable = MutableSharedFlow<ShortArray>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val currentStatus get() = recordingStatusMutable.replayCache.firstOrNull()
    val recordingStatusFlow: SharedFlow<RecordingStatus> = recordingStatusMutable
    val samplesFlow: SharedFlow<ShortArray> = samplesFlowMutable.asSharedFlow()

    @Suppress("MemberVisibilityCanBePrivate")
    val samplesCount get() = samplesCountAtomic.get()
    val samplesCountFlow: Flow<Long>
        get() {
            return samplesFlow.map {
                samplesCount
            }
        }

    init {
        recordingStatusMutable.tryEmit(RecordingStatus.IDLE)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording() {
        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // sanity check
        this.audioRecord?.let {
            stopRecording()
        }
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        audioRecord.setRecordPositionUpdateListener(object :
            AudioRecord.OnRecordPositionUpdateListener {
            private val bytesBuffer = ByteArray(bufferSize)
            override fun onMarkerReached(recorder: AudioRecord?) {
                // no-op
            }

            override fun onPeriodicNotification(recorder: AudioRecord?) {
                // don't use provided recorder because it can be outdated
                val read = this@AudioService.audioRecord?.read(bytesBuffer, 0, bytesBuffer.size)
                // TODO: handle error
                // send crash report if all bytes == 0 for few times
                if (read == null || read < bytesBuffer.size) {
                    // TODO: handle error
                    return
                }
                val shorts = ShortArray(bytesBuffer.size / 2)
                ByteBuffer.wrap(bytesBuffer, 0, bytesBuffer.size).order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer().get(shorts)
                samplesCountAtomic.addAndGet(shorts.size.toLong())
                samplesFlowMutable.tryEmit(shorts)
            }
        })
        audioRecord.setPositionNotificationPeriod(bufferSize / 2)
        audioRecord.startRecording()
        this.audioRecord = audioRecord
        samplesCountAtomic.set(0)
        recordingStatusMutable.tryEmit(RecordingStatus.RECORDING)
    }

    fun stopRecording() {
        recordingStatusMutable.tryEmit(RecordingStatus.IDLE)
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}