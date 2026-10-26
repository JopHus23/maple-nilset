package com.mapherdf.maplenilset.bili.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl

class BiliLoginApi(private val client: OkHttpClient) : BiliLoginApiService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchLoginState(): BiliLoginState = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/web-interface/nav")
            .get()
            .build()
        val body = readBody(request)
        val root = runCatching { json.parseToJsonElement(body).jsonObject }
            .getOrNull()
            ?: throw BiliLoginException(BiliLoginError.VALIDATION_FAILED)
        val code = root.long("code")
        if (code == NOT_LOGGED_IN_CODE) {
            return@withContext BiliLoginState(level = BiliLoginLevel.LOGGED_OUT)
        }
        if (code != 0L) {
            throw BiliLoginException(BiliLoginError.VALIDATION_FAILED)
        }
        val data = root["data"]?.jsonObject
            ?: throw BiliLoginException(BiliLoginError.VALIDATION_FAILED)
        val isLogin = data.boolean("isLogin") == true
        if (!isLogin) {
            return@withContext BiliLoginState(level = BiliLoginLevel.LOGGED_OUT)
        }
        val isVip = data.long("vipStatus") == 1L
        BiliLoginState(
            level = if (isVip) BiliLoginLevel.VIP_MEMBER else BiliLoginLevel.NORMAL_USER,
            nickname = data.string("uname"),
            avatarUrl = data.string("face"),
            mid = data.long("mid"),
        )
    }

    override suspend fun fetchMissingFingerprint(): Map<String, String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.bilibili.com/x/frontend/finger/spi")
            .get()
            .build()
        val body = readBody(request)
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: throw BiliLoginException(BiliLoginError.NETWORK_ERROR)
        if (root.long("code") != 0L) {
            throw BiliLoginException(BiliLoginError.NETWORK_ERROR)
        }
        val data = root["data"]?.jsonObject
            ?: throw BiliLoginException(BiliLoginError.NETWORK_ERROR)
        buildMap {
            data.string("b_3")?.let { put("buvid3", it) }
            data.string("b_4")?.let { put("buvid4", it) }
        }
    }

    private suspend fun readBody(request: Request): String = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw BiliLoginException(BiliLoginError.NETWORK_ERROR)
            }
            response.body?.string() ?: throw BiliLoginException(BiliLoginError.NETWORK_ERROR)
        }
    }

    private fun JsonObject.long(name: String): Long? =
        this[name]?.jsonPrimitive?.longOrNull

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }

    private fun JsonObject.boolean(name: String): Boolean? =
        this[name]?.jsonPrimitive?.booleanOrNull

    private companion object {
        private const val NOT_LOGGED_IN_CODE = -101L
    }
}

internal class BiliLoginException(val error: BiliLoginError) :
    IllegalStateException("Bili login request failed")
