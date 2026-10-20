package com.kidsee.core.network.updater

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class ReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String
)

class GitHubUpdateClient(
    private val repoOwner: String = "danilpromsiz-byte",
    private val repoName: String = "KidSee"
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkLatestRelease(customRepo: String? = null): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val targetRepo = customRepo ?: "$repoOwner/$repoName"
            val apiUrl = "https://api.github.com/repos/$targetRepo/releases/latest"
            val url = URL(apiUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "KidSee-Android-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            if (connection.responseCode != 200) {
                Timber.w("GitHub release check returned status: ${connection.responseCode}")
                return@withContext null
            }

            val responseBody = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val root = json.parseToJsonElement(responseBody).jsonObject

            val tagName = root["tag_name"]?.jsonPrimitive?.content ?: return@withContext null
            val releaseName = root["name"]?.jsonPrimitive?.content ?: tagName
            val releaseNotes = root["body"]?.jsonPrimitive?.content ?: ""

            var apkUrl: String? = null
            val assets = root["assets"]?.jsonArray
            if (assets != null) {
                for (asset in assets) {
                    val assetObj = asset.jsonObject
                    val name = assetObj["name"]?.jsonPrimitive?.content ?: ""
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content
                        break
                    }
                }
            }

            if (apkUrl != null) {
                ReleaseInfo(
                    tagName = tagName,
                    releaseName = releaseName,
                    releaseNotes = releaseNotes,
                    apkDownloadUrl = apkUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking GitHub release")
            null
        }
    }

    suspend fun downloadApk(
        downloadUrl: String,
        targetFile: java.io.File,
        onProgress: (Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (targetFile.exists()) targetFile.delete()
            targetFile.parentFile?.mkdirs()

            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirects = 0

            while (redirects < 5) {
                val url = URL(currentUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15000
                conn.readTimeout = 45000
                conn.setRequestProperty("User-Agent", "KidSee-Android-App")
                conn.connect()
                val code = conn.responseCode
                if (code in 301..308) {
                    val newLoc = conn.getHeaderField("Location")
                    if (!newLoc.isNullOrBlank()) {
                        currentUrl = newLoc
                        redirects++
                        conn.disconnect()
                        continue
                    }
                }
                connection = conn
                break
            }

            if (connection == null || connection.responseCode !in 200..299) {
                Timber.e("Failed to connect to download APK: \${connection?.responseCode}")
                return@withContext false
            }

            val totalBytes = connection.contentLength.toLong()
            var readBytes = 0L
            var lastUpdate = 0L

            connection.inputStream.use { input ->
                java.io.FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(16384)
                    var read = input.read(buffer)
                    while (read > 0) {
                        output.write(buffer, 0, read)
                        readBytes += read
                        val now = System.currentTimeMillis()
                        if (totalBytes > 0 && now - lastUpdate > 150) {
                            lastUpdate = now
                            val pct = ((readBytes * 100) / totalBytes).toInt().coerceIn(0, 99)
                            onProgress(pct)
                        }
                        read = input.read(buffer)
                    }
                    output.flush()
                }
            }

            if (targetFile.exists() && targetFile.length() > 50_000L) {
                targetFile.setReadable(true, false)
                onProgress(100)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error downloading APK from $downloadUrl")
            false
        }
    }
}
