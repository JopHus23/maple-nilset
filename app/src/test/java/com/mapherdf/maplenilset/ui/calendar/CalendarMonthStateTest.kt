package com.mapherdf.maplenilset.ui.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarMonthStateTest {
    private val today = LocalDate.of(2026, 9, 3)

    @Test
    fun `september grid starts on monday column`() {
        val state = CalendarMonthState(year = 2026, month = 9, today = today)

        assertEquals(1, state.leadingEmptyCells)
        assertEquals(30, state.daysInMonth)
        assertEquals(LocalDate.of(2026, 9, 1), state.date(1))
        assertEquals(LocalDate.of(2026, 9, 30), state.date(30))
    }

    @Test
    fun `today matches only current date`() {
        val state = CalendarMonthState(year = 2026, month = 9, today = today)

        assertTrue(state.isToday(3))
        assertFalse(state.isToday(2))
        assertFalse(state.isToday(4))
    }

    @Test
    fun `month navigation crosses year boundaries`() {
        val state = CalendarMonthState(year = 2026, month = 9, today = today)

        assertEquals(8 to 2026, state.previous().let { it.month to it.year })

        val december = CalendarMonthState(year = 2026, month = 12, today = today).next()
        assertEquals(1 to 2027, december.month to december.year)
    }
}
