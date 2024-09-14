package solutions.s4y.agora.services.agora

import android.content.Context
import android.util.Log
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.Constants.POSITION_RECORD
import io.agora.rtc2.Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY
import io.agora.rtc2.IAudioFrameObserver
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.audio.AudioParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import solutions.s4y.agora.preferences.AppIdPreference
import solutions.s4y.agora.preferences.ChannelNamePreference
import solutions.s4y.agora.preferences.ChannelTokenPreference
import solutions.s4y.agora.preferences.UserIdPreference
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


class AgoraPCMProvider(tag: String = "AgoraPCMProvider", onPCM: (ByteBuffer) -> Unit) : Closeable {
    sealed class ChannelEvent {
        data object NotJoinedChannel : ChannelEvent()
        class JoinedChannel(val channel: String) : ChannelEvent()
        class UserJoined(val uid: Int) : ChannelEvent()
        class UserOffline(val uid: Int) : ChannelEvent();

    }

    private val mutableChannelEventFlow = MutableStateFlow<ChannelEvent>(notJoined)
    private val mutableChannelJoinedFlow = MutableStateFlow<ChannelEvent>(notJoined)
    private val mutableMicFlow = MutableStateFlow(false)
    private val mutableEngineFlow = MutableStateFlow(false)

    /*
    private val mutablePCMSharedFlow = MutableSharedFlow<ByteBuffer>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST //SUSPEND
    )*/
    private val isVoice = AtomicBoolean(false)

    private var mRtcEngine: RtcEngine? = null

    private val mRtcEventHandler: IRtcEngineEventHandler = object : IRtcEngineEventHandler() {
        // Callback when successfully joining the channel
        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d(tag, "onJoinChannelSuccess $channel, $uid, $elapsed")
            super.onJoinChannelSuccess(channel, uid, elapsed)
            mutableChannelEventFlow.value = ChannelEvent.JoinedChannel(channel)
            mutableChannelJoinedFlow.value = ChannelEvent.JoinedChannel(channel)
        }

        // Callback when a remote user or host joins the current channel
        // Listen for remote hosts in the channel to get the host's uid information
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(tag, "onUserJoined $uid, $elapsed")
            super.onUserJoined(uid, elapsed)
            mutableChannelEventFlow.value = ChannelEvent.UserJoined(uid)
            mutableChannelJoinedFlow.value = ChannelEvent.UserJoined(uid)
        }

        // Callback when a remote user or host leaves the current channel
        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(tag, "onUserOffline $uid, $reason")
            super.onUserOffline(uid, reason)
            mutableChannelEventFlow.value = ChannelEvent.UserOffline(uid)
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            Log.d(tag, "onLeaveChannel ${stats?.connectTimeMs}")
            super.onLeaveChannel(stats)
            mutableChannelEventFlow.value = notJoined
            mutableChannelJoinedFlow.value = notJoined
        }

        private val volume0Count = AtomicInteger(0)
        private val previousVolume = AtomicInteger(0)
        override fun onAudioVolumeIndication(
            speakers: Array<AudioVolumeInfo>,
            totalVolume: Int
        ) {
            if (totalVolume < 10) {
                val count = volume0Count.incrementAndGet()
                if (count > 3) {
                    isVoice.set(false)
                    if (previousVolume.getAndSet(0) != 0) {
                        Log.d(tag, "onAudioVolumeIndication off")
                    }
                }
            } else {
                volume0Count.set(0)
                isVoice.set(true)
                if (previousVolume.getAndSet(totalVolume) == 0) {
                    Log.d(tag, "onAudioVolumeIndication on $totalVolume")
                }
            }
        }
    }

    private val mAudioFrameObserver = object : IAudioFrameObserver {
        override fun onRecordAudioFrame(
            channelId: String?,
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsync_type: Int
        ): Boolean {
            if (buffer != null) {
                if (isVoice.get()) {

                    Log.d(
                        tag,
                        "onRecordAudioFrame buffer: ${buffer.remaining()}"
                    )
                    onPCM(buffer)
                }
            }
            return false
        }

        override fun onPlaybackAudioFrame(
            channelId: String?,
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsync_type: Int
        ): Boolean {
            // Log.d(TAG, "onPlaybackAudioFrame")
            return false
        }

        override fun onMixedAudioFrame(
            channelId: String?,
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsync_type: Int
        ): Boolean {
            // Log.d(TAG, "onMixedAudioFrame(channelId: $channelId, type: $type, samplesPerChannel: $samplesPerChannel, bytesPerSample: $bytesPerSample, channels: $channels, samplesPerSec: $samplesPerSec, renderTimeMs: $renderTimeMs, avsync_type: $avsync_type)")
            return false
        }

        override fun onEarMonitoringAudioFrame(
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsync_type: Int
        ): Boolean {
            // Log.d(TAG, "onEarMonitoringAudioFrame")
            return false
        }

        override fun onPlaybackAudioFrameBeforeMixing(
            channelId: String?,
            uid: Int,
            type: Int,
            samplesPerChannel: Int,
            bytesPerSample: Int,
            channels: Int,
            samplesPerSec: Int,
            buffer: ByteBuffer?,
            renderTimeMs: Long,
            avsync_type: Int
        ): Boolean {
            // Log.d(TAG, "onPlaybackAudioFrameBeforeMixing")
            return false
        }

        override fun getObservedAudioFramePosition(): Int {
            Log.d(tag, "getObservedAudioFramePosition")
            return POSITION_RECORD
        }

        override fun getRecordAudioParams(): AudioParams {
            Log.d(tag, "getRecordAudioParams")
            return AudioParams(
                SAMPLE_RATE,
                SAMPLE_NUM_OF_CHANNEL,
                RAW_AUDIO_FRAME_OP_MODE_READ_ONLY,
                SAMPLES_PER_CALL
            )
        }

        override fun getPlaybackAudioParams(): AudioParams {
            Log.d(tag, "getPlaybackAudioParams")
            TODO()
        }

        override fun getMixedAudioParams(): AudioParams {
            Log.d(tag, "getMixedAudioParams")
            TODO()
        }

        override fun getEarMonitoringAudioParams(): AudioParams {
            Log.d(tag, "getEarMonitoringAudioParams")
            TODO()
        }

    }

    val channelEventFlow: StateFlow<ChannelEvent> = mutableChannelEventFlow
    val engineStateFlow: StateFlow<Boolean> = mutableEngineFlow
    val micFlow: StateFlow<Boolean> = mutableMicFlow
    val stateFlow: StateFlow<ChannelEvent> = mutableChannelJoinedFlow

    override fun close() {
        mRtcEngine?.let {
            it.leaveChannel()
            mRtcEngine = null
            RtcEngine.destroy()
        }
        mutableChannelJoinedFlow.value = notJoined
    }

    fun joinChannel(context: Context) {
        if (mRtcEngine == null) {
            open(context)
        }
        val options = ChannelMediaOptions()
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
        options.publishMicrophoneTrack = true
        options.autoSubscribeAudio = true
        mRtcEngine?.joinChannel(
            ChannelTokenPreference(context).value,
            ChannelNamePreference(context).value,
            UserIdPreference(context).value,
            options
        )
    }

    fun leaveChannel() {
        mRtcEngine?.leaveChannel()
    }

    fun micOn() {
        mRtcEngine?.enableLocalAudio(true)
        mutableMicFlow.value = true
    }

    fun micOff() {
        mRtcEngine?.enableLocalAudio(false)
        mutableMicFlow.value = false
    }

    fun open(context: Context) {
        if (mRtcEngine != null) {
            close()
        }
        val config = RtcEngineConfig()
        config.mContext = context
        config.mAppId = AppIdPreference(context).value
        config.mEventHandler = mRtcEventHandler

        mRtcEngine = RtcEngine.create(config).apply {
            // 750ms - at least 1 time per second, but not very often
            enableAudioVolumeIndication(750, 3, true)
            /* do we need this if registerAudioFrameObserver set ?
            setRecordingAudioFrameParameters(
                SAMPLE_RATE,
                SAMPLE_NUM_OF_CHANNEL,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_WRITE,
                SAMPLES_PER_CALL
            );
            setMixedAudioFrameParameters(SAMPLE_RATE, SAMPLE_NUM_OF_CHANNEL, SAMPLES_PER_CALL);
            setPlaybackAudioFrameParameters(
                SAMPLE_RATE,
                SAMPLE_NUM_OF_CHANNEL,
                Constants.RAW_AUDIO_FRAME_OP_MODE_READ_WRITE,
                SAMPLES_PER_CALL
            );
             */
            registerAudioFrameObserver(mAudioFrameObserver)
        }
    }

    companion object {
        internal val notJoined = ChannelEvent.NotJoinedChannel
        private const val SAMPLE_RATE = 16000
        private const val SAMPLE_NUM_OF_CHANNEL = 1
        private const val SAMPLES_PER_CALL = SAMPLE_RATE
    }
}