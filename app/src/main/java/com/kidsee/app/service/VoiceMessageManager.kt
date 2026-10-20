package com.kidsee.app.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import com.kidsee.core.domain.model.VoiceMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Handles recording voice notes on parent device and playing them with ducking on child device.
 */
class VoiceMessageManager(private val context: Context) {

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null

    // --- Child Device: Play incoming voice message ---

    fun playVoiceMessage(message: VoiceMessage, onFinished: () -> Unit = {}) {
        Timber.d("Playing voice message from ${message.senderName}")

        // 1. Request audio focus to duck/pause media playback
        val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .build()
        } else null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest)
        }

        // 2. Play audio file
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(message.audioUrl)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setVolume(1.0f, 1.0f)
                setOnCompletionListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                        audioManager.abandonAudioFocusRequest(focusRequest)
                    }
                    onFinished()
                }
                setOnErrorListener { _, what, extra ->
                    Timber.e("MediaPlayer error playing voice: what=$what, extra=$extra")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
                        audioManager.abandonAudioFocusRequest(focusRequest)
                    }
                    onFinished()
                    true
                }
                prepareAsync()
                setOnPreparedListener {
                    start()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error playing voice message")
            onFinished()
        }
    }

    // --- Parent Device: Record voice note ---

    suspend fun startRecording(): File? = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = outputFile

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            Timber.d("Recording started: ${outputFile.name}")
            outputFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to start audio recording")
            null
        }
    }

    suspend fun stopRecording(): File? = withContext(Dispatchers.IO) {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            Timber.d("Recording stopped: ${currentRecordingFile?.length()} bytes")
            currentRecordingFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to stop recording")
            null
        }
    }
}
