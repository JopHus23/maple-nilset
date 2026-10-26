package com.mapherdf.maplenilset.bili.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.mapherdf.maplenilset.bili.api.BiliCookieStore
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class BiliLoginManager(
    private val cookieStore: BiliCookieStore,
    loginApi: BiliLoginApiService,
    private val webCookieStore: BiliWebCookieStore,
) : BiliLoginClient {

    private val api = loginApi
    private val operationMutex = Mutex()
    private val _loginState = MutableStateFlow(BiliLoginState())
    override val loginState: StateFlow<BiliLoginState> = _loginState.asStateFlow()

    override suspend fun importWebViewCookies(): BiliLoginResult = operationMutex.withLock {
        webCookieStore.flush()
        val imported = WEB_COOKIE_URLS.mapNotNull { url ->
            webCookieStore.readCookie(url)?.let { url to parseCookieString(it) }
        }
        val merged = mergedCookies(imported)
        if (merged.isNotEmpty()) {
            cookieStore.importCookieValues(merged)
        }
        ensureFingerprint()
        validateCookies()
    }

    override suspend fun importCookieString(raw: String, sourceUrl: String): BiliLoginResult {
        val url = sourceUrl.toHttpUrlOrNull()
        if (url == null || !BiliCookieStore.isBiliHost(url.host)) {
            return failure(BiliLoginError.INVALID_SOURCE)
        }
        return operationMutex.withLock {
            val values = parseCookieString(raw)
            if (values.isEmpty()) {
                return@withLock failure(BiliLoginError.EMPTY_COOKIES)
            }
            cookieStore.importCookieValues(values)
            ensureFingerprint()
            validateCookies()
        }
    }

    override suspend fun refreshLoginState(): BiliLoginResult = operationMutex.withLock {
        if (!cookieStore.hasLoginCookie()) {
            _loginState.update { BiliLoginState() }
            return@withLock success(BiliLoginState())
        }
        validateCookies()
    }

    override suspend fun logout() = operationMutex.withLock {
        cookieStore.clear()
        val webCookieNames = mutableSetOf<String>()
        (WEB_COOKIE_URLS + WEB_LOGOUT_URLS).forEach { url ->
            webCookieStore.readCookie(url)
                ?.let(::parseCookieString)
                ?.keys
                ?.forEach(webCookieNames::add)
        }
        webCookieNames.forEach { name ->
            WEB_COOKIE_URLS.forEach { url ->
                webCookieStore.expireCookie(url, name)
            }
            WEB_LOGOUT_URLS.forEach { url ->
                webCookieStore.expireCookie(url, name, WEB_COOKIE_DOMAIN)
            }
        }
        webCookieStore.flush()
        _loginState.value = BiliLoginState()
    }

    private suspend fun validateCookies(): BiliLoginResult {
        _loginState.update { it.copy(isValidating = true) }
        return try {
            val state = api.fetchLoginState()
            _loginState.value = state
            if (state.level == BiliLoginLevel.LOGGED_OUT) {
                failure(state, BiliLoginError.NOT_LOGGED_IN)
            } else {
                success(state)
            }
        } catch (exception: BiliLoginException) {
            failure(_loginState.value, exception.error)
        } catch (exception: Exception) {
            if (exception is kotlinx.coroutines.CancellationException) {
                throw exception
            }
            failure(_loginState.value, BiliLoginError.NETWORK_ERROR)
        } finally {
            _loginState.update { it.copy(isValidating = false) }
        }
    }

    private suspend fun ensureFingerprint() {
        if (cookieStore.hasCookie("buvid3") && cookieStore.hasCookie("buvid4")) {
            return
        }
        runCatching {
            val values = api.fetchMissingFingerprint().filterValues { it.isNotBlank() }
            cookieStore.importCookieValues(values)
        }
    }

    private fun mergedCookies(sourceCookies: List<Pair<String, Map<String, String>>>): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        sourceCookies
            .sortedBy { (url, _) -> WEB_COOKIE_URLS.indexOf(url) }
            .forEach { (_, values) ->
                values.forEach { (name, value) ->
                    if (!result.containsKey(name)) {
                        result[name] = value
                    }
                }
            }
        return result
    }

    private fun parseCookieString(raw: String): Map<String, String> {
        return raw.split(";")
            .mapNotNull { segment ->
                val separator = segment.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val name = segment.take(separator).trim()
                val value = segment.substring(separator + 1).trim()
                if (name.isBlank() || value.isBlank()) null else name to value
            }
            .toMap()
    }

    private fun success(state: BiliLoginState) = BiliLoginResult(state, success = true)

    private fun failure(error: BiliLoginError) = failure(_loginState.value, error)

    private fun failure(state: BiliLoginState, error: BiliLoginError) =
        BiliLoginResult(state, success = false, error = error)

    private companion object {
        private val WEB_COOKIE_URLS = listOf(
            "https://passport.bilibili.com/",
            "https://api.bilibili.com/",
            "https://www.bilibili.com/",
        )
        private val WEB_LOGOUT_URLS = listOf(
            "https://bilibili.com/",
        )
        private const val WEB_COOKIE_DOMAIN = ".bilibili.com"
    }
}
