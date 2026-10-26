package com.mapherdf.maplenilset.bili.api

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class MemoryBiliCookiePersistence : BiliCookiePersistence {
    var stored: List<BiliStoredCookie> = emptyList()

    override fun load(): List<BiliStoredCookie> = stored
    override fun save(cookies: List<BiliStoredCookie>) {
        stored = cookies
    }

    override fun clear() {
        stored = emptyList()
    }
}

class BiliCookieStoreTest {

    private val persistence = MemoryBiliCookiePersistence()
    private val store = BiliCookieStore(persistence)

    @Test
    fun `imported cookies are sent only to bilibili hosts`() {
        store.importCookieValues(
            mapOf(
                "SESSDATA" to "session",
                "buvid3" to "buvid-three",
            ),
        )

        listOf(
            "https://www.bilibili.com/",
            "https://passport.bilibili.com/",
            "https://api.bilibili.com/",
        ).forEach { url ->
            assertTrue(store.loadForRequest(url.toHttpUrl()).any { it.name == "SESSDATA" })
        }

        listOf(
            "https://upos-sz-mirror08c.bilivideo.com/",
            "https://i0.hdslb.com/bfs/archive/test.jpg",
            "https://example.akamaized.net/",
        ).forEach { url ->
            assertTrue(store.loadForRequest(url.toHttpUrl()).isEmpty())
        }
    }

    @Test
    fun `cookies from non bilibili hosts are rejected`() {
        val cdnUrl = "https://upos-sz-mirror08c.bilivideo.com/".toHttpUrl()
        val cookie = Cookie.Builder()
            .name("SESSDATA")
            .value("session")
            .domain("bilibili.com")
            .build()

        store.saveFromResponse(cdnUrl, listOf(cookie))

        assertTrue(persistence.stored.isEmpty())
        assertTrue(store.loadForRequest("https://api.bilibili.com/".toHttpUrl()).isEmpty())
    }

    @Test
    fun `imported cookie replaces matching storage key`() {
        store.importCookieValues(mapOf("SESSDATA" to "old"))
        store.importCookieValues(mapOf("SESSDATA" to "new"))

        val stored = persistence.stored.single()
        assertEquals("SESSDATA", stored.name)
        assertEquals("new", stored.value)
        assertEquals(1, persistence.stored.size)
    }

    @Test
    fun `clear removes persisted credentials`() {
        store.importCookieValues(mapOf("SESSDATA" to "session"))
        store.clear()

        assertTrue(persistence.stored.isEmpty())
        assertFalse(store.hasLoginCookie())
        assertNull(store.loadForRequest("https://api.bilibili.com/".toHttpUrl()).firstOrNull())
    }
}
