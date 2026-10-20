package com.kidsee.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.kidsee.app.MainActivity
import com.kidsee.core.ui.R
import com.kidsee.core.network.webrtc.WebRtcManager
import timber.log.Timber

/**
 * Foreground Service for capturing screen video and system audio via MediaProjection API.
 */
class ScreenCaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "kidsee_screen_capture"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        var isRunning = false
            private set
    }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var systemAudioRecord: AudioRecord? = null
    private var webRtcManager: WebRtcManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != 0 && resultData != null) {
            // Android 14+ requires startForeground with FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            // BEFORE initializing MediaProjection!
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            isRunning = true
            initMediaProjection(resultCode, resultData)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun initMediaProjection(resultCode: Int, resultData: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, resultData)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                stopCapture()
            }
        }, null)

        webRtcManager = WebRtcManager(this)
        val videoTrack = webRtcManager?.initVideoTrack()

        // Capture system audio on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
            try {
                systemAudioRecord = webRtcManager?.createSystemAudioRecord(mediaProjection!!)
                systemAudioRecord?.startRecording()
                Timber.d("System audio recording started")
            } catch (e: Exception) {
                Timber.e(e, "Could not start AudioPlaybackCapture")
            }
        }

        // Setup VirtualDisplay
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        // Virtual display feeds video frames
        Timber.d("Screen capture initialized: ${width}x${height} @ $density dpi")
    }

    private fun stopCapture() {
        try {
            systemAudioRecord?.stop()
            systemAudioRecord?.release()
            systemAudioRecord = null

            virtualDisplay?.release()
            virtualDisplay = null

            mediaProjection?.stop()
            mediaProjection = null

            webRtcManager?.release()
            webRtcManager = null
        } catch (e: Exception) {
            Timber.e(e, "Error stopping screen capture")
        }
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies when KidSee is sharing screen and audio with parents"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KidSee is Active")
            .setContentText("Screen and audio streaming to parent")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
