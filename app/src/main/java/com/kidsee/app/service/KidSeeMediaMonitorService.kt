package com.kidsee.app.service

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import timber.log.Timber

/**
 * Uses NotificationListenerService + MediaSessionManager
 * to control media playback (pause, play, skip, previous) in OTHER apps on the child device.
 */
class KidSeeMediaMonitorService : NotificationListenerService() {

    companion object {
        var instance: KidSeeMediaMonitorService? = null
            private set
    }

    private val mediaSessionManager by lazy {
        getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    }

    private val audioManager by lazy {
        getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.d("KidSeeMediaMonitorService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun getActiveControllers(): List<MediaController> {
        return try {
            val component = ComponentName(this, KidSeeMediaMonitorService::class.java)
            mediaSessionManager.getActiveSessions(component)
        } catch (e: SecurityException) {
            Timber.w(e, "Notification access not granted for getActiveSessions")
            emptyList()
        }
    }

    fun pauseMedia() {
        val controllers = getActiveControllers()
        Timber.d("Pausing media across ${controllers.size} sessions")
        controllers.forEach { controller ->
            controller.transportControls.pause()
        }
    }

    fun playMedia() {
        val controllers = getActiveControllers()
        Timber.d("Playing media across ${controllers.size} sessions")
        controllers.firstOrNull()?.transportControls?.play()
    }

    fun skipNext() {
        val controllers = getActiveControllers()
        Timber.d("Skipping to next media")
        controllers.firstOrNull()?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        val controllers = getActiveControllers()
        Timber.d("Skipping to previous media")
        controllers.firstOrNull()?.transportControls?.skipToPrevious()
    }

    fun setVolume(percent: Int) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val targetVolume = (percent * maxVolume) / 100
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
        } catch (_: Exception) {}
        val controllers = getActiveControllers()
        for (c in controllers) {
            try {
                c.setVolumeTo(targetVolume, AudioManager.FLAG_SHOW_UI)
            } catch (_: Exception) {}
        }
    }

    fun changeVolumeBy(delta: Int) {
        val direction = if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        try {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            audioManager.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        } catch (_: Exception) {}
        val controllers = getActiveControllers()
        for (c in controllers) {
            try {
                c.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
            } catch (_: Exception) {}
        }
    }

    fun getCurrentVolumePercent(): Int {
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) (current * 100) / max else 50
    }

    fun isMediaPlaying(): Boolean {
        val controllers = getActiveControllers()
        return controllers.any { it.playbackState?.state == PlaybackState.STATE_PLAYING }
    }
}
