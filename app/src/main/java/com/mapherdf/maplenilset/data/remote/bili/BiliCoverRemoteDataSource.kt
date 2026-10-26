package com.mapherdf.maplenilset.data.remote.bili

import android.util.Base64
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.mapherdf.maplenilset.const.BiliExpressions
import com.mapherdf.maplenilset.core.bili.BiliContentKind
import com.mapherdf.maplenilset.core.bili.BiliInputParser
import com.mapherdf.maplenilset.di.BiliCacheDirectory
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiliCoverRemoteDataSource @Inject constructor(
    @BiliCacheDirectory private val cacheDirectory: File,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val redirectClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()
    private val contentClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @WorkerThread
    suspend fun resolve(input: BiliInputReference): BiliCoverDetails = withContext(Dispatchers.IO) {
        when (input.kind) {
            BiliContentKind.AV -> requestVideo("aid", input.id, input)
            BiliContentKind.BV -> requestVideo("bvid", "BV${input.id}", input)
            BiliContentKind.LIVE -> requestLive(input)
        }
    }

    @WorkerThread
    suspend fun resolveShortLink(code: String): BiliInputReference = withContext(Dispatchers.IO) {
        val startUrl = "https://b23.tv/$code".toHttpUrl()
        var currentUrl: okhttp3.HttpUrl = startUrl
        repeat(MAX_SHORT_LINK_HOPS) {
            val request = jsonRequest(currentUrl).head().build()
            redirectClient.newCall(request).execute().use { response ->
                val location = response.header("location")
                    ?.let { currentUrl.resolve(it)?.toString() }
                if (response.isRedirect && location != null) {
                    val next = BiliInputParser.parseContent(location)
                    if (next != null) return@withContext BiliInputReference(next.kind, next.id)
                    currentUrl = location.toHttpUrl()
                    return@use
                }
            }
            throw BiliCoverException(
                BiliExpressions.GENERIC_UPSTREAM_CODE,
                "b23 short link could not be resolved",
            )
        }
        throw BiliCoverException(
            BiliExpressions.GENERIC_UPSTREAM_CODE,
            "b23 short link has too many redirects",
        )
    }

    @WorkerThread
    suspend fun downloadCover(
        rawUrl: String,
        displayName: String,
    ): BiliCoverFile = withContext(Dispatchers.IO) {
        val url = normalizeImageUrl(rawUrl)?.toHttpUrlOrNull()
        if (url == null) {
            throw BiliCoverException(BiliExpressions.GENERIC_UPSTREAM_CODE, "Cover URL is not allowed")
        }

        cacheDirectory.mkdirs()
        val destination = File(
            cacheDirectory,
            "${sha256(displayName)}-${System.nanoTime()}.bin",
        )
        val request = contentClient.requestBuilder(url).get().build()
        contentClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw BiliCoverException(response.code, "Cover download failed")
            }
            val body = response.body ?: throw IOException("Empty cover response")
            val declaredMime = body.contentType()?.toString()
            destination.outputStream().use { output ->
                val input = body.byteStream()
        val head = ByteArray(16)
                var headLength = 0
                while (headLength < head.size) {
                    val length = input.read(head, headLength, head.size - headLength)
                    if (length < 0) break
                    headLength += length
                }
                output.write(head, 0, headLength)
                input.copyTo(output)
                output.flush()
            }

            val mimeType = resolveMimeType(declaredMime, destination)
                ?: throw BiliCoverException(
                    BiliExpressions.GENERIC_UPSTREAM_CODE,
                    "Response is not a supported image",
                )
            BiliCoverFile(
                file = destination,
                mimeType = mimeType,
                extension = extensionFor(mimeType),
            )
        }
    }

    private fun requestVideo(
        parameterName: String,
        parameterValue: String,
        input: BiliInputReference,
    ): BiliCoverDetails {
        val url = "https://api.bilibili.com/x/web-interface/view"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter(parameterName, parameterValue)
            .build()
        val response = readJson(url, BiliVideoEnvelope.serializer())
        val data = response.data
            ?: throw BiliCoverException(
                BiliExpressions.GENERIC_UPSTREAM_CODE,
                "Upstream response has no content",
            )
        val imageUrl = normalizeImageUrl(data?.pic)
        if (imageUrl == null) {
            throw BiliCoverException(
                BiliExpressions.GENERIC_UPSTREAM_CODE,
                "Upstream response has no allowed cover",
            )
        }
        return BiliCoverDetails(
            input = input,
            title = data.title.orEmpty(),
            imageUrl = imageUrl,
            author = data.owner?.name,
            uid = data.owner?.mid,
            description = data.desc?.takeIf { it.isNotBlank() },
        )
    }



    private fun requestLive(input: BiliInputReference): BiliCoverDetails {
        val form = FormBody.Builder().add("id", input.id).build()
        val request = jsonRequest("https://api.live.bilibili.com/room/v1/Room/get_info".toHttpUrl())
            .post(form)
            .build()
        val response = readJson(request, BiliLiveEnvelope.serializer())
        val data = response.data
            ?: throw BiliCoverException(
                BiliExpressions.GENERIC_UPSTREAM_CODE,
                "Upstream response has no content",
            )
        val imageUrl = normalizeImageUrl(data?.user_cover)
        if (imageUrl == null) {
            throw BiliCoverException(
                BiliExpressions.GENERIC_UPSTREAM_CODE,
                "Upstream response has no allowed cover",
            )
        }
        return BiliCoverDetails(
            input = input,
            title = data.title.orEmpty(),
            imageUrl = imageUrl,
            uid = data.uid,
            author = null,
            description = null,
        )
    }

    private fun <T> readJson(
        url: okhttp3.HttpUrl,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T where T : BiliEnvelope {
        val request = jsonRequest(url).get().build()
        return readJson(request, serializer)
    }

    private fun <T> readJson(
        request: Request,
        serializer: kotlinx.serialization.KSerializer<T>,
    ): T where T : BiliEnvelope {
        contentClient.newCall(request).execute().use { response ->
            val bodyText = response.body?.string() ?: throw IOException("Empty upstream response")
            if (!response.isSuccessful) {
                throw BiliCoverException(response.code, bodyText)
            }
            val result = runCatching {
                json.decodeFromString(serializer, bodyText)
            }.getOrElse { throw BiliCoverException(BiliExpressions.GENERIC_UPSTREAM_CODE, "Invalid upstream JSON") }
            if (result.code != 0) {
                throw BiliCoverException(result.code, result.message ?: "Upstream request failed")
            }
            return result
        }
    }

    private fun jsonRequest(url: okhttp3.HttpUrl): Request.Builder =
        contentClient.requestBuilder(url)
            .header("Accept", BiliExpressions.ACCEPT_JSON)

    private fun OkHttpClient.requestBuilder(url: okhttp3.HttpUrl): Request.Builder =
        Request.Builder()
            .url(url)
            .header("User-Agent", BiliExpressions.USER_AGENT)
            .header("Referer", BiliExpressions.REFERER)

    private fun normalizeImageUrl(rawUrl: String?): String? {
        val url = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val parsed = url.toHttpUrlOrNull() ?: return null
        val isTrustedHost = parsed.host == BiliExpressions.IMAGE_HOST ||
            parsed.host.endsWith(".${BiliExpressions.IMAGE_HOST}", ignoreCase = true)
        if (!isTrustedHost) return null
        return parsed.newBuilder().scheme("https").build().toString()
    }

    private fun resolveMimeType(declaredMime: String?, file: File): String? {
        declaredMime?.substringBefore(';')?.trim()?.lowercase()?.let { mime ->
            if (mime in SUPPORTED_IMAGE_MIMES) return mime
        }
        val head = ByteArray(16)
        file.inputStream().use { stream ->
            var offset = 0
            while (offset < head.size) {
                val length = stream.read(head, offset, head.size - offset)
                if (length < 0) break
                offset += length
            }
        }
        return when {
            head.startsWithBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) -> "image/png"
            head.startsWithBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())) -> "image/jpeg"
            head.startsWithBytes("RIFF".toByteArray()) &&
                head.copyOfRange(8, minOf(12, head.size)).contentEquals("WEBP".toByteArray()) -> "image/webp"
            head.startsWithBytes("GIF".toByteArray()) -> "image/gif"
            else -> null
        }
    }

    private fun ByteArray.startsWithBytes(prefix: ByteArray): Boolean =
        size >= prefix.size && copyOfRange(0, prefix.size).contentEquals(prefix)

    private fun extensionFor(mimeType: String): String = when (mimeType) {
        "image/jpeg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> "jpg"
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.NO_PADDING)
            .replace('/', '_')
            .replace('+', '-')
            .trimEnd('=')
    }

    companion object {
        private const val MAX_SHORT_LINK_HOPS = 3
        private val SUPPORTED_IMAGE_MIMES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
        )
    }
}
