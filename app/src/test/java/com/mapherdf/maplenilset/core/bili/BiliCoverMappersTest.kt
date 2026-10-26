package com.mapherdf.maplenilset.core.bili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BiliCoverMappersTest {

    @Test
    fun `allows only https bilibili image hosts`() {
        assertTrue(BiliCoverMappers.isAllowedImageUrl("https://i0.hdslb.com/bfs/archive/a.jpg"))
        assertFalse(BiliCoverMappers.isAllowedImageUrl("http://i0.hdslb.com/a.jpg"))
        assertFalse(BiliCoverMappers.isAllowedImageUrl("https://evil.example/a.jpg"))
        assertFalse(BiliCoverMappers.isAllowedImageUrl("https://hdslb.com.evil.example/a.jpg"))
        assertFalse(BiliCoverMappers.isAllowedImageUrl(null))
    }

    @Test
    fun `builds safe file names`() {
        assertEquals(
            "nilset-bili-cover-av-170001.jpg",
            BiliCoverMappers.coverFileName(BiliContentKind.AV, "170001", "jpg"),
        )
        assertEquals(
            "nilset-bili-cover-bv-1xx.webp",
            BiliCoverMappers.coverFileName(BiliContentKind.BV, "1xx", ".webp"),
        )
    }
}
