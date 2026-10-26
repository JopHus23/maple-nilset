package com.mapherdf.maplenilset.bili.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.mapherdf.maplenilset.bili.api.BiliApiException
import com.mapherdf.maplenilset.bili.api.BiliLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

class BiliChunkDownloader(
    private val client: OkHttpClient,
) {

    suspend fun downloadToFile(
        url: String,
        destination: File,
        alreadyDownloaded: Long,
        totalExpected: Long,
        onProgress: (Long) -> Unit,
        alternateUrl: String? = null,
    ): Long = withContext(Dispatchers.IO) {
        var attempts = 0
        while (attempts <= MAX_RETRIES) {
            try {
                return@withContext attemptSinglePass(url, destination, alreadyDownloaded, onProgress)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts++
                logFailure(url, e, attempts)
                if (attempts > MAX_RETRIES) throw e
                delay(BASE_BACKOFF_MS * (1L shl attempts) + Random.nextLong(JITTER_MIN_MS, JITTER_MAX_MS))
            }
        }
        throw BiliApiException(-1, "Download failed after $MAX_RETRIES retries")
    }

    private fun attemptSinglePass(
        url: String,
        destination: File,
        alreadyDownloaded: Long,
        onProgress: (Long) -> Unit,
    ): Long {
        destination.parentFile?.mkdirs()
        BiliLogger.d(TAG, "Download starting: offset=$alreadyDownloaded dest=${destination.name}")
        val builder = Request.Builder().url(url)
        if (alreadyDownloaded > 0) builder.header("Range", "bytes=$alreadyDownloaded-")
        val request = builder.build()
        client.newCall(request).execute().use { response ->
            val code = response.code
            BiliLogger.d(TAG, "Response: code=$code message=${response.message}")
            if (code == 403) throw BiliApiException(403, "CDN returned 403")
            if (!response.isSuccessful && code != 206) {
                val bodySnippet = response.body?.string()?.take(200) ?: "(no body)"
                throw BiliApiException(code, "HTTP $code body=${bodySnippet}")
            }
            val body = response.body ?: throw BiliApiException(-1, "Empty download body")
            var downloaded = alreadyDownloaded
            FileOutputStream(destination, alreadyDownloaded > 0).use { output ->
                if (alreadyDownloaded > 0) output.channel.position(alreadyDownloaded)
                val buffer = ByteArray(CHUNK_SIZE)
                while (true) {
                    if (Thread.currentThread().isInterrupted) throw CancellationException("Interrupted")
                    val read = body.byteStream().read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded)
                }
            }
            BiliLogger.d(TAG, "Download complete: $downloaded bytes")
            return downloaded
        }
    }

    private fun logFailure(url: String, e: Exception, attempt: Int) {
        val maskedUrl = url.substringBefore("?") + "?" + (url.substringAfter("?").take(30)) + "..."
        BiliLogger.e(TAG, "Download attempt $attempt failed: ${e.javaClass.simpleName} ${e.message} url=$maskedUrl", e)
    }

    companion object {
        private const val TAG = "BiliChunkDownloader"
        private const val MAX_RETRIES = 3
        private const val BASE_BACKOFF_MS = 1000L
        private const val JITTER_MIN_MS = 100L
        private const val JITTER_MAX_MS = 300L
        private const val CHUNK_SIZE = 1 shl 20
    }
}

class LinkExpiredException(
    val expiredUrl: String,
    cause: Throwable,
) : Exception("CDN link expired: $expiredUrl", cause)