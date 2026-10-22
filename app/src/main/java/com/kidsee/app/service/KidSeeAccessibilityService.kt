package com.kidsee.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kidsee.app.KidSeeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Monitors the child's foreground activity and inspects UI nodes
 * to detect whether video content is playing (YouTube, TikTok, Netflix, players)
 * or if a non-video application is active.
 */
class KidSeeAccessibilityService : AccessibilityService() {

    companion object {
        var instance: KidSeeAccessibilityService? = null
            private set
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastPackageName: String? = null
    private var lastVideoDetected: Boolean = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Always guarantee KidSeeChildService is running in foreground when child device is used
        if (!KidSeeChildService.isRunning) {
            KidSeeChildService.start(applicationContext)
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkgName = event.packageName?.toString() ?: return
                if (pkgName == packageName) return // Ignore own app

                lastPackageName = pkgName
                val appLabel = getAppLabel(pkgName)
                val isVideo = detectVideoApp(pkgName)

                updateDeviceActivity(pkgName, appLabel, isVideo)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val root = rootInActiveWindow ?: return
                val isVideoPlaying = inspectNodesForVideo(root)
                if (isVideoPlaying != lastVideoDetected) {
                    lastVideoDetected = isVideoPlaying
                    lastPackageName?.let { pkg ->
                        updateDeviceActivity(pkg, getAppLabel(pkg), isVideoPlaying)
                    }
                }
            }
        }
    }

    private fun detectVideoApp(packageName: String): Boolean {
        val knownVideoPackages = listOf(
            "youtube",
            "netflix",
            "disney",
            "amazon.avod",
            "musically", // TikTok
            "tiktok",
            "vlc",
            "videoplayer",
            "twitch",
            "rutube",
            "kinopoisk",
            "ivi.client",
            "okko",
            "kion",
            "premier",
            "vk.video"
        )
        return knownVideoPackages.any { packageName.contains(it, ignoreCase = true) }
    }

    private fun inspectNodesForVideo(node: AccessibilityNodeInfo): Boolean {
        // Search for video player indicators: pause button, play button, progress bar
        val viewIdsToLookFor = listOf(
            "player", "video", "play_pause", "pause_button", "exo_pause", "exo_play"
        )

        for (id in viewIdsToLookFor) {
            val nodes = node.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) return true
        }

        // Check for content descriptions
        val descriptions = listOf("Pause", "Play", "Pause video", "Video player", "Пауза", "Воспроизведение")
        for (desc in descriptions) {
            val nodes = node.findAccessibilityNodeInfosByText(desc)
            if (nodes.isNotEmpty()) return true
        }

        return false
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun updateDeviceActivity(packageName: String, appLabel: String, isVideo: Boolean) {
        serviceScope.launch {
            try {
                KidSeeApp.instance.stateManager.onChildActivityChanged(
                    packageName = packageName,
                    appLabel = appLabel,
                    isVideo = isVideo
                )
            } catch (e: Exception) {
                Timber.e(e, "Error updating child activity")
            }
        }
    }

    override fun onInterrupt() {
        Timber.d("AccessibilityService interrupted")
    }

    fun lockDeviceScreen(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val ok = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Timber.d("Accessibility performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN): $ok")
                ok
            } else {
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error locking screen via AccessibilityService")
            false
        }
    }

    fun goToHomeScreen(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } catch (e: Exception) {
            Timber.e(e, "Error returning home via AccessibilityService")
            false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        KidSeeChildService.start(applicationContext)
        Timber.d("KidSee Accessibility Service Connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
