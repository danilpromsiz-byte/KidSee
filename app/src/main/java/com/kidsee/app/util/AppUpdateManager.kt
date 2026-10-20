package com.kidsee.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.kidsee.core.network.updater.GitHubUpdateClient
import com.kidsee.core.network.updater.ReleaseInfo
import com.kidsee.core.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val releaseInfo: ReleaseInfo) : UpdateState()
    object UpToDate : UpdateState()
    data class Downloading(val progressPercent: Int) : UpdateState()
    data class ReadyToInstall(val apkFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class AppUpdateManager(
    private val context: Context,
    private val updateClient: GitHubUpdateClient = GitHubUpdateClient()
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

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

    suspend fun downloadAndInstall(releaseInfo: ReleaseInfo) {
        val targetFile = File(context.cacheDir, "KidSee-update.apk")
        _updateState.value = UpdateState.Downloading(0)

        val success = updateClient.downloadApk(
            downloadUrl = releaseInfo.apkDownloadUrl,
            targetFile = targetFile,
            onProgress = { pct ->
                _updateState.value = UpdateState.Downloading(pct)
            }
        )

        if (success && targetFile.exists()) {
            _updateState.value = UpdateState.ReadyToInstall(targetFile)
            installApk(targetFile)
        } else {
            _updateState.value = UpdateState.Error("APK download failed. Check network connection.")
        }
    }

    fun installApk(apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch package installer")
            _updateState.value = UpdateState.Error("Installation failed: ${e.message}")
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
