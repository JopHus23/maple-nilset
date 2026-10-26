package com.mapherdf.maplenilset.bili.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiliMediaExporterTest {

    @Test
    fun sanitizeReplacesIllegalCharacters() {
        assertEquals("hello_world.mp4", BiliMediaExporter.sanitizeFilename("hello:world"))
        assertEquals("a_b_c.mp4", BiliMediaExporter.sanitizeFilename("a/b\\c"))
    }

    @Test
    fun sanitizeTrimsDotsAndSpaces() {
        assertEquals("file.mp4", BiliMediaExporter.sanitizeFilename(" file.mp4 "))
        assertEquals("test.mp4", BiliMediaExporter.sanitizeFilename(".test."))
    }

    @Test
    fun sanitizeAppendsMp4IfMissing() {
        assertEquals("video.mp4", BiliMediaExporter.sanitizeFilename("video"))
        assertEquals("video.mp4", BiliMediaExporter.sanitizeFilename("video.mp4"))
    }

    @Test
    fun sanitizeBlankInputReturnsDefault() {
        assertEquals("___.mp4", BiliMediaExporter.sanitizeFilename("///"))
        assertEquals("nilset_video.mp4", BiliMediaExporter.sanitizeFilename("  "))
    }
}