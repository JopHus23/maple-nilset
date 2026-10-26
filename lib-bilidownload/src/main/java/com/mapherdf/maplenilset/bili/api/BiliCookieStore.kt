package com.mapherdf.maplenilset.bili.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class BiliCookieStore(
    private val persistence: BiliCookiePersistence,
) : CookieJar {

    private val lock = Any()
    private var cookies: List<BiliStoredCookie> = persistence.load()

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!isBiliHost(url.host)) {
            return emptyList()
        }
        return synchronized(lock) {
            cookies.mapNotNull(::toOkHttpCookie).filter { it.matches(url) }
        }
    }

    override fun saveFromResponse(url: HttpUrl, responseCookies: List<Cookie>) {
        if (!isBiliHost(url.host)) {
            return
        }
        synchronized(lock) {
            responseCookies.filter { it.domain.isBiliDomain() }.forEach { cookie ->
                replaceLocked(
                    BiliStoredCookie(
                        name = cookie.name,
                        value = cookie.value,
                        domain = cookie.domain,
                        path = cookie.path,
                        expiresAt = cookie.expiresAt,
                        secure = cookie.secure,
                        httpOnly = cookie.httpOnly,
                        hostOnly = cookie.hostOnly,
                    ),
                )
            }
            persistLocked()
        }
    }

    fun importCookieValues(values: Map<String, String>) {
        if (values.isEmpty()) {
            return
        }
        synchronized(lock) {
            values.forEach { (name, value) ->
                replaceLocked(
                    BiliStoredCookie(
                        name = name,
                        value = value,
                        domain = BILI_DOMAIN,
                        path = "/",
                        secure = true,
                    ),
                )
            }
            persistLocked()
        }
    }

    fun hasCookie(name: String): Boolean = synchronized(lock) {
        cookies.any { it.name == name }
    }

    fun hasLoginCookie(): Boolean = hasCookie(LOGIN_COOKIE_NAME)

    fun clear() {
        synchronized(lock) {
            cookies = emptyList()
            persistence.clear()
        }
    }

    private fun replaceLocked(cookie: BiliStoredCookie) {
        val key = cookie.storageKey
        cookies = cookies.filterNot { it.storageKey == key } + cookie
    }

    private fun persistLocked() {
        persistence.save(cookies)
    }

    private fun toOkHttpCookie(cookie: BiliStoredCookie): Cookie? {
        return runCatching {
            Cookie.Builder()
                .name(cookie.name)
                .value(cookie.value)
                .apply {
                    if (cookie.hostOnly) {
                        hostOnlyDomain(cookie.domain)
                    } else {
                        domain(cookie.domain)
                    }
                }
                .path(cookie.path)
                .apply {
                    if (cookie.secure) secure()
                    if (cookie.httpOnly) httpOnly()
                    if (cookie.expiresAt > 0L) expiresAt(cookie.expiresAt)
                }
                .build()
        }.getOrNull()
    }

    private val BiliStoredCookie.storageKey: String
        get() = "${domain.lowercase()}|$path|$name"

    private fun String.isBiliDomain(): Boolean {
        val normalized = removePrefix(".").lowercase()
        return normalized == "bilibili.com" || normalized.endsWith(".bilibili.com")
    }

    companion object {
        private const val BILI_DOMAIN = "bilibili.com"
        private const val LOGIN_COOKIE_NAME = "SESSDATA"

        fun isBiliHost(host: String): Boolean {
            val normalized = host.lowercase()
            return normalized == "bilibili.com" || normalized.endsWith(".bilibili.com")
        }
    }
}
