package com.mapherdf.maplenilset.bili.auth

import kotlinx.coroutines.test.runTest
import com.mapherdf.maplenilset.bili.api.BiliCookiePersistence
import com.mapherdf.maplenilset.bili.api.BiliCookieStore
import com.mapherdf.maplenilset.bili.api.BiliStoredCookie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemoryCookiePersistence : BiliCookiePersistence {
    var stored: List<BiliStoredCookie> = emptyList()
    override fun load(): List<BiliStoredCookie> = stored
    override fun save(cookies: List<BiliStoredCookie>) {
        stored = cookies
    }

    override fun clear() {
        stored = emptyList()
    }
}

private class FakeBiliLoginApi : BiliLoginApiService {
    var state: BiliLoginState = BiliLoginState()
    val fingerprint: MutableMap<String, String> = mutableMapOf()
    var fingerprintCalls = 0

    override suspend fun fetchLoginState(): BiliLoginState = state

    override suspend fun fetchMissingFingerprint(): Map<String, String> {
        fingerprintCalls += 1
        return fingerprint
    }
}

private class FakeWebCookieStore : BiliWebCookieStore {
    private val cookies = mutableMapOf<String, String>()
    val expiredNames = mutableSetOf<String>()
    val expiredDomainCookies = mutableListOf<Pair<String, String?>>()
    var flushed = 0

    fun put(url: String, cookie: String) {
        cookies[url] = cookie
    }

    override fun flush() {
        flushed += 1
    }

    override fun readCookie(url: String): String? = cookies[url]

    override fun expireCookie(url: String, name: String, domain: String?) {
        expiredNames += name
        expiredDomainCookies += name to domain
        cookies.remove(url)
    }
}

class BiliLoginManagerTest {

    private fun createStore(): Pair<BiliCookieStore, MemoryCookiePersistence> {
        val persistence = MemoryCookiePersistence()
        return BiliCookieStore(persistence) to persistence
    }

    @Test
    fun `import missing fingerprint then reports vip member`() = runTest {
        val (store, persistence) = createStore()
        val api = FakeBiliLoginApi()
        api.fingerprint["buvid3"] = "three"
        api.fingerprint["buvid4"] = "four"
        api.state = BiliLoginState(
            level = BiliLoginLevel.VIP_MEMBER,
            nickname = "user",
            avatarUrl = "https://i0.hdslb.com/avatar.jpg",
            mid = 42L,
        )
        val manager = BiliLoginManager(store, api, FakeWebCookieStore())

        val result = manager.importCookieString("SESSDATA=session", "https://passport.bilibili.com/")

        assertTrue(result.success)
        assertEquals(BiliLoginLevel.VIP_MEMBER, result.state.level)
        assertEquals("user", result.state.nickname)
        assertEquals(1, api.fingerprintCalls)
        assertTrue(store.hasCookie("buvid3"))
        assertTrue(store.hasCookie("buvid4"))
        assertTrue(persistence.stored.any { it.name == "SESSDATA" })
    }

    @Test
    fun `manual import rejects non bilibili source`() = runTest {
        val (store, persistence) = createStore()
        val api = FakeBiliLoginApi()
        val manager = BiliLoginManager(store, api, FakeWebCookieStore())

        val result = manager.importCookieString("SESSDATA=session", "https://example.com/")

        assertFalse(result.success)
        assertEquals(BiliLoginError.INVALID_SOURCE, result.error)
        assertTrue(persistence.stored.isEmpty())
    }

    @Test
    fun `webview cookies from three domains merge by source priority`() = runTest {
        val (store, persistence) = createStore()
        val webStore = FakeWebCookieStore()
        webStore.put("https://passport.bilibili.com/", "SESSDATA=passport")
        webStore.put("https://api.bilibili.com/", "SESSDATA=api")
        webStore.put("https://www.bilibili.com/", "SESSDATA=www; buvid3=three")
        val api = FakeBiliLoginApi()
        api.state = BiliLoginState(level = BiliLoginLevel.NORMAL_USER)
        val manager = BiliLoginManager(store, api, webStore)

        val result = manager.importWebViewCookies()

        assertTrue(result.success)
        assertEquals(BiliLoginLevel.NORMAL_USER, result.state.level)
        assertEquals("passport", persistence.stored.single { it.name == "SESSDATA" }.value)
        assertTrue(persistence.stored.any { it.name == "buvid3" })
    }

    @Test
    fun `logout clears store and expires webview cookies`() = runTest {
        val (store, persistence) = createStore()
        val webStore = FakeWebCookieStore()
        webStore.put("https://api.bilibili.com/", "SESSDATA=session; bili_jct=token")
        val api = FakeBiliLoginApi()
        val manager = BiliLoginManager(store, api, webStore)
        store.importCookieValues(mapOf("SESSDATA" to "session"))

        manager.logout()

        assertTrue(persistence.stored.isEmpty())
        assertFalse(store.hasLoginCookie())
        assertTrue(webStore.expiredNames.contains("SESSDATA"))
        assertTrue(webStore.expiredNames.contains("bili_jct"))
        assertTrue(
            webStore.expiredDomainCookies.contains(
                "SESSDATA" to ".bilibili.com",
            ),
        )
        assertTrue(
            webStore.expiredDomainCookies.contains(
                "bili_jct" to ".bilibili.com",
            ),
        )
    }
}
