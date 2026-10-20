package com.kidsee.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
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

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastPackageName: String? = null
    private var lastVideoDetected: Boolean = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

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
            "com.google.android.youtube",
            "com.google.android.apps.youtube.kids",
            "com.netflix.mediaclient",
            "com.disney.disneyplus",
            "com.amazon.avod.thirdpartyclient",
            "com.zhiliaoapp.musically", // TikTok
            "org.videolan.vlc",
            "com.mxtech.videoplayer.ad",
            "tv.twitch.android.app"
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("KidSee Accessibility Service Connected")
    }
}
