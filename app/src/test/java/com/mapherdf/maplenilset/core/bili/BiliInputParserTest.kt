package com.mapherdf.maplenilset.core.bili

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BiliInputParserTest {

    @Test
    fun `parses bare video ids`() {
        assertEquals(BiliInput(BiliContentKind.AV, "170001"), BiliInputParser.parse(" 170001 "))
        assertEquals(BiliInput(BiliContentKind.AV, "170001"), BiliInputParser.parse("AV170001"))
        assertEquals(BiliInput(BiliContentKind.BV, "1xx411c7mD"), BiliInputParser.parse("BV1xx411c7mD"))
    }

    @Test
    fun `parses desktop mobile and short urls`() {
        assertEquals(
            BiliInput(BiliContentKind.AV, "170001"),
            BiliInputParser.parse("https://www.bilibili.com/video/av170001?p=1"),
        )
        assertEquals(
            BiliInput(BiliContentKind.BV, "1xx411c7mD"),
            BiliInputParser.parse("https://www.bilibili.com/video/BV1xx411c7mD/"),
        )
        assertEquals(
            BiliInput(BiliContentKind.LIVE, "2233"),
            BiliInputParser.parse("https://live.bilibili.com/2233?from=search"),
        )
    }

    @Test
    fun `rejects unsupported and malformed input`() {
        assertNull(BiliInputParser.parse(""))
        assertNull(BiliInputParser.parse("https://example.com/video/av1"))
        assertNull(BiliInputParser.parse("https://b23.tv/"))
        assertNull(BiliInputParser.parse("BV"))
        assertNull(BiliInputParser.parse("av12a"))
    }

    @Test
    fun `separates short link codes from content parsing`() {
        assertEquals("1AbC", BiliInputParser.parseShortLinkCode("https://b23.tv/1AbC?share_source=copy_link"))
        assertNull(BiliInputParser.parse("https://b23.tv/1AbC"))
        assertNull(BiliInputParser.parseShortLinkCode("https://www.bilibili.com/video/av170001"))
    }
}
