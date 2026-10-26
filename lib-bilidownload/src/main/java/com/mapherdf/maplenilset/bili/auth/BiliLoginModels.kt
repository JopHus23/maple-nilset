package com.mapherdf.maplenilset.bili.auth

import kotlinx.coroutines.flow.StateFlow

enum class BiliLoginLevel {
    LOGGED_OUT,
    NORMAL_USER,
    VIP_MEMBER,
}

data class BiliLoginState(
    val level: BiliLoginLevel = BiliLoginLevel.LOGGED_OUT,
    val nickname: String? = null,
    val avatarUrl: String? = null,
    val mid: Long? = null,
    val isValidating: Boolean = false,
)

enum class BiliLoginError {
    INVALID_SOURCE,
    EMPTY_COOKIES,
    NOT_LOGGED_IN,
    NETWORK_ERROR,
    VALIDATION_FAILED,
}

data class BiliLoginResult(
    val state: BiliLoginState,
    val success: Boolean,
    val error: BiliLoginError? = null,
)

interface BiliWebCookieStore {
    fun flush()
    fun readCookie(url: String): String?
    fun expireCookie(url: String, name: String, domain: String? = null)
}

interface BiliLoginApiService {
    suspend fun fetchLoginState(): BiliLoginState
    suspend fun fetchMissingFingerprint(): Map<String, String>
}

interface BiliLoginClient {
    val loginState: StateFlow<BiliLoginState>
    suspend fun importWebViewCookies(): BiliLoginResult
    suspend fun importCookieString(raw: String, sourceUrl: String): BiliLoginResult
    suspend fun refreshLoginState(): BiliLoginResult
    suspend fun logout()
}
