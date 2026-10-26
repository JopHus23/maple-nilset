package com.mapherdf.maplenilset.ui.console

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsoleScrollTest {
    @Test
    fun `empty content is considered at bottom`() {
        assertTrue(isConsoleScrolledToBottom(scrollValue = 0, maxValue = 0))
    }

    @Test
    fun `scroll at maximum is considered at bottom`() {
        assertTrue(isConsoleScrolledToBottom(scrollValue = 180, maxValue = 180))
    }

    @Test
    fun `scroll above bottom is not considered at bottom`() {
        assertFalse(isConsoleScrolledToBottom(scrollValue = 100, maxValue = 180))
    }
}
