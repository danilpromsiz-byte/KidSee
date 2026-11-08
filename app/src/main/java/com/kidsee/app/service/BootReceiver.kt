package com.kidsee.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kidsee.core.data.local.KidSeePreferences
import com.kidsee.core.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Automatically launches KidSee child monitoring service on system startup, package update,
 * screen unlock, or after memory reclamation/task removal.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_RESTART_SERVICE = "com.kidsee.app.RESTART_CHILD_SERVICE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_USER_PRESENT ||
            action == ACTION_RESTART_SERVICE
        ) {
            Timber.d("BootReceiver received action: $action")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = KidSeePreferences(context)
                    val role = prefs.roleFlow.firstOrNull()
                    if (role == UserRole.CHILD) {
                        Timber.d("BootReceiver starting KidSeeChildService for child device")
                        KidSeeChildService.start(context)
                        ChildKeepAliveWorker.schedule(context)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error checking role in BootReceiver")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
