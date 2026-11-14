package com.mapherdf.maplenilset.core.bili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BiliInputParserTest {

    @Test
    fun `parse extracts bv av and live inputs`() {
        assertEquals(
            BiliInput(BiliContentKind.BV, "1xx411c7mD"),
            BiliInputParser.parseContent("https://www.bilibili.com/video/BV1xx411c7mD?p=1"),
        )
        assertEquals(
            BiliInput(BiliContentKind.AV, "170001"),
            BiliInputParser.parseContent("av170001"),
        )
        assertEquals(
            BiliInput(BiliContentKind.LIVE, "123456"),
            BiliInputParser.parseContent("https://live.bilibili.com/h5/123456"),
        )
    }

    @Test
    fun `parses bare video ids`() {
        assertEquals(BiliInput(BiliContentKind.AV, "170001"), BiliInputParser.parse(" 170001 "))
        assertEquals(BiliInput(BiliContentKind.AV, "170001"), BiliInputParser.parse("AV170001"))
        assertEquals(BiliInput(BiliContentKind.BV, "1xx411c7mD"), BiliInputParser.parse("BV1xx411c7mD"))
    }

    @Test
    fun `parse extracts short link from shared text`() {
        val input = "【原神】宣传片 https://b23.tv/abc123d ?share_source=copy"

        assertEquals("abc123d", BiliInputParser.parseShortLinkCode(input))
        assertNull(BiliInputParser.parse(input))
    }

    @Test
    fun `parse accepts short link without scheme`() {
        assertEquals("abc123d", BiliInputParser.parseShortLinkCode("b23.tv/abc123d"))
    }

    @Test
    fun `rejects unsupported and malformed input`() {
        assertNull(BiliInputParser.parse(""))
        assertNull(BiliInputParser.parse("https://example.com/video/av1"))
        assertNull(BiliInputParser.parse("https://b23.tv/"))
        assertNull(BiliInputParser.parse("BV"))
        assertNull(BiliInputParser.parse("av12a"))
    }
}
