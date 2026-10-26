package com.mapherdf.maplenilset.bili.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.time.LocalDate

class BiliWbiSigner(
    private val client: OkHttpClient,
) {

    private val mutex = Mutex()

    @Volatile
    private var cachedMixinKey: String? = null

    @Volatile
    private var cachedDate: LocalDate? = null

    suspend fun sign(parameters: Map<String, String>): Map<String, String> = withContext(Dispatchers.IO) {
        val mixinKey = obtainMixinKey()
        val timestamp = (System.currentTimeMillis() / MILLIS_PER_SECOND).toString()
        val allParams = parameters + (PARAM_WTS to timestamp)
        val sorted = allParams.toSortedMap()
        val filtered = sorted.mapValues { (_, value) -> stripReserved(value) }
        val query = filtered.entries.joinToString(AMP) { (key, value) ->
            "${rfc3986Encode(key)}=${rfc3986Encode(value)}"
        }
        val signature = md5Hex(query + mixinKey)
        filtered + (PARAM_W_RID to signature)
    }

    suspend fun invalidate() {
        mutex.withLock {
            cachedMixinKey = null
            cachedDate = null
        }
    }

    private suspend fun obtainMixinKey(): String {
        val today = LocalDate.now()
        cachedMixinKey?.let { key ->
            if (cachedDate == today) return key
        }
        return mutex.withLock {
            cachedMixinKey?.let { key ->
                if (cachedDate == today) return key
            }
            val fresh = fetchMixinKey()
            cachedMixinKey = fresh
            cachedDate = today
            fresh
        }
    }

    private fun fetchMixinKey(): String {
        val request = Request.Builder()
            .url(URL_NAV)
            .header("User-Agent", BiliHttpHeaders.USER_AGENT)
            .header("Referer", BiliHttpHeaders.REFERER)
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw BiliApiException(-1, "Empty nav response")
            if (!response.isSuccessful) throw BiliApiException(response.code, "Nav request failed: ${response.code}")
            val json = JSONObject(body)
            val data = json.getJSONObject(FIELD_DATA)
            val wbiImg = data.getJSONObject(FIELD_WBI_IMG)
            val imgUrl = wbiImg.getString(FIELD_IMG_URL)
            val subUrl = wbiImg.getString(FIELD_SUB_URL)
            val rawKey = extractFileNameWithoutExtension(imgUrl) + extractFileNameWithoutExtension(subUrl)
            return rawKey.take(MIXIN_KEY_LENGTH)
        }
    }

    companion object {
        private const val URL_NAV = "https://api.bilibili.com/x/web-interface/nav"
        private const val FIELD_DATA = "data"
        private const val FIELD_WBI_IMG = "wbi_img"
        private const val FIELD_IMG_URL = "img_url"
        private const val FIELD_SUB_URL = "sub_url"
        private const val PARAM_WTS = "wts"
        private const val PARAM_W_RID = "w_rid"
        private const val AMP = "&"
        private const val MILLIS_PER_SECOND = 1000L
        private const val MIXIN_KEY_LENGTH = 32

        private val RESERVED_CHARS = charArrayOf('!', '\'', '(', ')', '*')

        internal val MIXIN_KEY_REORDER_TABLE = intArrayOf(
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
        )

        internal fun extractFileNameWithoutExtension(url: String): String =
            url.substringAfterLast('/').substringBeforeLast('.')

        internal fun stripReserved(value: String): String {
            var result = value
            for (char in RESERVED_CHARS) {
                result = result.replace(char.toString(), "")
            }
            return result
        }

        internal fun rfc3986Encode(value: String): String {
            val builder = StringBuilder()
            val bytes = value.toByteArray(Charsets.UTF_8)
            for (byte in bytes) {
                val c = byte.toInt().toChar()
if ((c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') || c in "-_.~") {
                    builder.append(c)
                } else {
                    builder.append('%')
                    builder.append(String.format("%02X", byte.toInt() and 0xFF))
                }
            }
            return builder.toString()
        }

        internal fun md5Hex(value: String): String {
            val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { String.format("%02x", it) }
        }
    }
}
