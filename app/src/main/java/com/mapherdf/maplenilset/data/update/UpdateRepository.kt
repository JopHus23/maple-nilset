package com.mapherdf.maplenilset.data.update

import android.content.Context
import com.mapherdf.maplenilset.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class GitHubReleaseAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
)

@Serializable
data class GitHubRelease(
    val tagName: String,
    val assets: List<GitHubReleaseAsset> = emptyList(),
)

data class UpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
)

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(API_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        val response = okHttpClient.newCall(request).execute()
        response.use {
            if (!it.isSuccessful) return@withContext null
            val body = it.body?.string() ?: return@withContext null
            val release = json.decodeFromString<GitHubRelease>(body)
            val latestVersion = release.tagName.removePrefix("v")
            if (latestVersion <= BuildConfig.VERSION_NAME) return@withContext null
            val apkAsset = release.assets.firstOrNull { asset -> asset.name.endsWith(".apk") }
                ?: return@withContext null
            UpdateInfo(
                latestVersion = latestVersion,
                downloadUrl = apkAsset.browserDownloadUrl,
            )
        }
    }

    suspend fun downloadApk(
        url: String,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = okHttpClient.newCall(request).execute()
        response.use {
            check(it.isSuccessful) { "Download failed: ${'$'}{it.code}" }
            val body = it.body ?: error("Empty response body")
            val contentLength = body.contentLength()
            val destination = File(context.cacheDir, APK_FILE_NAME)
            body.byteStream().use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var totalRead = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (contentLength > 0) {
                            onProgress(((totalRead * 100) / contentLength).toInt())
                        }
                    }
                }
            }
            destination
        }
    }

    companion object {
        private const val API_URL =
            "https://api.github.com/repos/JopHus23/maple-nilset/releases/latest"
        private const val APK_FILE_NAME = "maple-nilset-update.apk"
        private const val BUFFER_SIZE = 8192
    }
}
