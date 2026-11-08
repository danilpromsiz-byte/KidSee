package com.kidsee.app.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kidsee.core.data.local.KidSeePreferences
import com.kidsee.core.domain.model.UserRole
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * 24/7 Background Watchdog Worker.
 * Ensures the KidSee child monitoring service is revived even if killed by aggressive OEM power managers.
 */
class ChildKeepAliveWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME = "KidSeeChildKeepAliveWorker"

        fun schedule(context: Context) {
            try {
                val workRequest = PeriodicWorkRequestBuilder<ChildKeepAliveWorker>(
                    15, TimeUnit.MINUTES
                ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest
                )
                Timber.d("ChildKeepAliveWorker periodic work scheduled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to schedule ChildKeepAliveWorker")
            }
        }

        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            } catch (e: Exception) {
                Timber.e(e, "Failed to cancel ChildKeepAliveWorker")
            }
        }
    }

    override suspend fun doWork(): Result {
        try {
            val prefs = KidSeePreferences(applicationContext)
            val role = prefs.roleFlow.firstOrNull()
            if (role == UserRole.CHILD) {
                if (!KidSeeChildService.isRunning) {
                    Timber.d("ChildKeepAliveWorker: KidSeeChildService was stopped, reviving!")
                    KidSeeChildService.start(applicationContext)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in ChildKeepAliveWorker doWork")
        }
        return Result.success()
    }
}
