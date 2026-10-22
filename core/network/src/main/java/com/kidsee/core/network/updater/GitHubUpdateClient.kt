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

            ReleaseInfo(
                tagName = tagName,
                releaseName = releaseName,
                releaseNotes = releaseNotes,
                apkDownloadUrl = ""
            )
        } catch (e: Exception) {
            Timber.e(e, "Error checking GitHub release")
            null
        }
    }
}
