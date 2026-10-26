package com.mapherdf.maplenilset.bili.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiliWbiSignerTest {

    @Test
    fun `extracts file name without extension from wbi key urls`() {
        val img = "https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png"
        val sub = "https://i0.hdslb.com/bfs/wbi/4932caff0ff746eab6f01bf08b70ac45.jpg"
        val combined = BiliWbiSigner.extractFileNameWithoutExtension(img) +
            BiliWbiSigner.extractFileNameWithoutExtension(sub)
        assertEquals("7cd084941338484aae1ad9425b84077c4932caff0ff746eab6f01bf08b70ac45", combined)
    }

    @Test
    fun `strips reserved characters from parameter values`() {
        assertEquals("test", BiliWbiSigner.stripReserved("test"))
        assertEquals("testvalue", BiliWbiSigner.stripReserved("test!value"))
        assertEquals("testvalue", BiliWbiSigner.stripReserved("test'value"))
        assertEquals("testvalue", BiliWbiSigner.stripReserved("test(value)"))
        assertEquals("testvalue", BiliWbiSigner.stripReserved("test*value"))
    }

    @Test
    fun `rfc3986 encodes space as percent 20 not plus`() {
        assertEquals("hello%20world", BiliWbiSigner.rfc3986Encode("hello world"))
        assertEquals("a-b.c_d~e", BiliWbiSigner.rfc3986Encode("a-b.c_d~e"))
        assertEquals("%E4%BD%A0%E5%A5%BD", BiliWbiSigner.rfc3986Encode("你好"))
    }

    @Test
    fun `md5 hex produces lowercase digest`() {
        val digest = BiliWbiSigner.md5Hex("test")
        assertEquals(32, digest.length)
        assertTrue(digest.all { it in "0123456789abcdef" })
        assertEquals("098f6bcd4621d373cade4e832627b4f6", digest)
    }
}