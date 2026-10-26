package com.mapherdf.maplenilset.ui.calendar

import java.time.LocalDate
import java.time.YearMonth

data class CalendarMonthState(
    val year: Int,
    val month: Int,
    private val today: LocalDate,
) {
    private val firstDay: LocalDate = LocalDate.of(year, month, 1)

    val leadingEmptyCells: Int = firstDay.dayOfWeek.value - 1

    val daysInMonth: Int = YearMonth.of(year, month).lengthOfMonth()

    fun date(dayOfMonth: Int): LocalDate = firstDay.withDayOfMonth(dayOfMonth)

    fun isToday(dayOfMonth: Int): Boolean = date(dayOfMonth) == today

    fun previous(): CalendarMonthState {
        val previousMonth = YearMonth.of(year, month).minusMonths(1)
        return copy(year = previousMonth.year, month = previousMonth.monthValue)
    }

    fun next(): CalendarMonthState {
        val nextMonth = YearMonth.of(year, month).plusMonths(1)
        return copy(year = nextMonth.year, month = nextMonth.monthValue)
    }
}
