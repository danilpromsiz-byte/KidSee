package com.kidsee.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.kidsee.core.network.updater.GitHubUpdateClient
import com.kidsee.core.network.updater.ReleaseInfo
import com.kidsee.core.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val releaseInfo: ReleaseInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AppUpdateManager(
    private val context: Context,
    private val updateClient: GitHubUpdateClient = GitHubUpdateClient()
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkForUpdates(isManual: Boolean = false) {
        _updateState.value = UpdateState.Checking
        if (isManual) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, context.getString(R.string.update_checking), Toast.LENGTH_SHORT).show()
            }
        }
        try {
            val release = updateClient.checkLatestRelease()
            if (release != null) {
                val currentVersion = getCurrentVersionName()
                val serverTag = release.tagName.removePrefix("v").trim()
                if (isNewerVersion(serverTag, currentVersion)) {
                    _updateState.value = UpdateState.UpdateAvailable(release)
                    return
                }
            }
            _updateState.value = UpdateState.UpToDate
            if (isManual) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "${context.getString(R.string.update_up_to_date)} (${getCurrentVersionName()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Check for updates failed")
            _updateState.value = UpdateState.Error("Failed to check updates: ${e.message}")
            if (isManual) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun openGooglePlay() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }

    private fun isNewerVersion(serverVersion: String, currentVersion: String): Boolean {
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
}
