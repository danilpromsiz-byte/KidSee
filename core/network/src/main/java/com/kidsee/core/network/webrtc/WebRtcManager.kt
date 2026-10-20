package com.kidsee.core.network.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import timber.log.Timber

/**
 * Manages WebRTC PeerConnection for streaming screen video and system audio
 * between child and parent devices.
 */
class WebRtcManager(private val context: Context) {

    val eglBase: EglBase by lazy { EglBase.create() }

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true, // enableIntelVp8Encoder
            true  // enableH264HighProfile
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var peerConnection: PeerConnection? = null

    // STUN / TURN servers for NAT traversal
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, observer)
        return peerConnection
    }

    // --- Child Device: Initialize Video & Audio Streams ---

    fun initVideoTrack(): VideoTrack {
        localVideoSource = peerConnectionFactory.createVideoSource(true) // isScreencast = true
        localVideoTrack = peerConnectionFactory.createVideoTrack("KIDSEE_SCREEN_TRACK", localVideoSource)
        localVideoTrack?.setEnabled(true)
        return localVideoTrack!!
    }

    fun initMicAudioTrack(): AudioTrack {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        localAudioSource = peerConnectionFactory.createAudioSource(constraints)
        localAudioTrack = peerConnectionFactory.createAudioTrack("KIDSEE_AUDIO_TRACK", localAudioSource)
        localAudioTrack?.setEnabled(true)
        return localAudioTrack!!
    }

    /**
     * Captures internal system audio (music, video, games) using AudioPlaybackCapture API (Android 10+).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun createSystemAudioRecord(mediaProjection: MediaProjection): AudioRecord {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        return AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(44100)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 2)
            .build()
    }

    fun addLocalTracksToConnection() {
        val pc = peerConnection ?: return
        localVideoTrack?.let { pc.addTrack(it) }
        localAudioTrack?.let { pc.addTrack(it) }
    }

    // --- Parent Device: Attach Remote Video to Renderer ---

    fun initRenderer(renderer: SurfaceViewRenderer) {
        renderer.init(eglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
    }

    fun release() {
        try {
            peerConnection?.close()
            peerConnection = null
            localVideoSource?.dispose()
            localAudioSource?.dispose()
            eglBase.release()
        } catch (e: Exception) {
            Timber.e(e, "Error releasing WebRTC manager")
        }
    }
}
