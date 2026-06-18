package com.spmods.spgram.data.datasource.remote

import android.util.Log
import com.spmods.spgram.domain.models.RichText
import com.spmods.spgram.domain.models.UpdateInfo
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches update info from a GitHub-hosted JSON file.
 *
 * Expected JSON format:
 * {
 *   "version": "1.2.3",
 *   "versionCode": 123,
 *   "description": "SPGram update",
 *   "changelog": ["Bug fixes", "New features"],
 *   "downloadUrl": "https://yourwebsite.com/spgram.apk",
 *   "fileName": "spgram-1.2.3.apk",
 *   "fileSize": 52428800,
 *   "forceUpdate": false
 * }
 */
class GitHubUpdateRemoteDataSource(
    private val jsonUrl: String = UPDATE_JSON_URL
) : UpdateRemoteDateSource {

    private val tag = "GitHubUpdateRemote"

    override suspend fun fetchLatestUpdate(): UpdateInfo? {
        return try {
            val json = fetchJson(jsonUrl)
            parseUpdateInfo(json)
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch update info", e)
            null
        }
    }

    private fun fetchJson(urlString: String): JSONObject {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("HTTP $responseCode fetching update JSON")
        }

        val body = connection.inputStream.bufferedReader().use { it.readText() }
        return JSONObject(body)
    }

    private fun parseUpdateInfo(json: JSONObject): UpdateInfo {
        val version = json.getString("version")
        val versionCode = json.getInt("versionCode")
        val description = json.optString("description", "")
        val downloadUrl = json.getString("downloadUrl")
        val fileName = json.optString("fileName", "spgram-$version.apk")
        val fileSize = json.optLong("fileSize", 0L)
        val forceUpdate = json.optBoolean("forceUpdate", false)

        val changelogArray = json.optJSONArray("changelog")
        val changelog = buildList {
            if (changelogArray != null) {
                for (i in 0 until changelogArray.length()) {
                    val text = changelogArray.getString(i)
                    if (text.isNotBlank()) add(RichText(text))
                }
            }
        }

        return UpdateInfo(
            version = version,
            versionCode = versionCode,
            description = description,
            changelog = changelog,
            downloadUrl = downloadUrl,
            fileName = fileName,
            fileSize = fileSize,
            forceUpdate = forceUpdate
        )
    }

    // getTdLibVersion and getTdLibCommitHash are no longer used with GitHub-based updates.
    // Return empty strings so the interface is satisfied without needing TDLib.
    override suspend fun getTdLibVersion(): String = ""
    override suspend fun getTdLibCommitHash(): String = ""

    companion object {
        // Replace with your actual GitHub raw JSON URL, e.g.:
        // https://raw.githubusercontent.com/YourUser/YourRepo/main/update.json
        const val UPDATE_JSON_URL =
            "https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/main/update.json"
    }
}
