package com.kidsee.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.kidsee.app.KidSeeApp
import com.kidsee.app.MainActivity
import com.kidsee.core.ui.R
import timber.log.Timber

/**
 * Permanent Foreground Service for Child device.
 * Ensures the device remains 100% online, alive in RAM, and receptive to parent commands,
 * live screen streaming, and intercom audio even when backgrounded or with screen turned off.
 */
class KidSeeChildService : Service() {

    companion object {
        const val ACTION_START = "com.kidsee.app.START_CHILD_SERVICE"
        const val ACTION_STOP = "com.kidsee.app.STOP_CHILD_SERVICE"
        const val CHANNEL_ID = "kidsee_child_persistent_channel"
        const val NOTIFICATION_ID = 1002

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, KidSeeChildService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to start KidSeeChildService")
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, KidSeeChildService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
        Timber.d("KidSeeChildService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true

        // Ensure state manager services and LAN discovery server are active
        try {
            val app = application as? KidSeeApp
            app?.stateManager?.startChildServices()
        } catch (e: Exception) {
            Timber.e(e, "Error starting child services from KidSeeChildService")
        }

        Timber.d("KidSeeChildService running in foreground (START_STICKY)")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        releaseLocks()
        Timber.d("KidSeeChildService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "KidSee::ChildServiceWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // 24 hours max safeguard
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "KidSee::ChildServiceWifiLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            Timber.w(e, "Could not acquire wake/wifi locks")
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            wakeLock = null
            if (wifiLock?.isHeld == true) wifiLock?.release()
            wifiLock = null
        } catch (e: Exception) {
            Timber.w(e, "Error releasing wake/wifi locks")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.child_persistent_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.child_persistent_service_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.child_persistent_notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
