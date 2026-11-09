package com.kidsee.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kidsee.app.MainActivity
import com.kidsee.app.R
import com.kidsee.app.util.AppUpdateManager
import com.kidsee.core.data.local.KidSeePreferences
import com.kidsee.core.network.updater.GitHubUpdateClient
import com.kidsee.core.ui.R as UiR
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically checks for app updates.
 * When an update is detected, it alerts the parent with a notification and persists the state.
 */
class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "KidSeeAppUpdateWorker"
        private const val CHANNEL_ID = "kidsee_updates_channel"
        private const val NOTIFICATION_ID = 2002

        fun schedule(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val workRequest = PeriodicWorkRequestBuilder<AppUpdateWorker>(
                    12, TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                Timber.d("AppUpdateWorker periodic check scheduled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to schedule AppUpdateWorker")
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val updateClient = GitHubUpdateClient()
            val releaseInfo = updateClient.checkLatestRelease()

            if (releaseInfo != null) {
                val rawTag = releaseInfo.tagName.trim().removePrefix("v").removePrefix("V")
                val currentVersion = AppUpdateManager(applicationContext).getCurrentVersionName().trim().removePrefix("v").removePrefix("V")

                if (isNewer(rawTag, currentVersion)) {
                    Timber.d("New app update found: $rawTag (current: $currentVersion)")
                    val prefs = KidSeePreferences(applicationContext)
                    prefs.setUpdateAvailableVersion(rawTag)
                    showUpdateNotification(rawTag)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "AppUpdateWorker failed during update check")
            Result.retry()
        }
    }

    private fun isNewer(serverVersion: String, currentVersion: String): Boolean {
        val serverParts = serverVersion.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentVersion.split(".").mapNotNull { it.toIntOrNull() }
        val length = maxOf(serverParts.size, currentParts.size)
        for (i in 0 until length) {
            val s = serverParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (s > c) return true
            if (s < c) return false
        }
        return false
    }

    private fun showUpdateNotification(version: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(UiR.string.update_notification_title),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "KidSee App Updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(UiR.string.update_notification_title))
            .setContentText(applicationContext.getString(UiR.string.update_notification_text, version))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
