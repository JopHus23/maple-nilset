package com.mapherdf.maplenilset.ui.schedule

import com.mapherdf.maplenilset.data.calendar.CalendarItem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleCourseSelectorTest {

    @Test
    fun `courses for selects exact weekday and sorts by start time`() {
        val records = listOf(
            course(weekday = 4, start = 1020, title = "English"),
            course(weekday = 1, start = 480, title = "Math"),
            course(weekday = 4, start = 900, title = "Physics"),
        )

        val result = ScheduleCourseSelector.coursesFor(records, 4)

        assertEquals(listOf("Physics", "English"), result.map { it.title })
    }

    @Test
    fun `next course returns today course after current minute`() {
        val result = ScheduleCourseSelector.nextCourse(
            records = listOf(course(weekday = 3, start = 900, title = "Math")),
            today = LocalDate.of(2026, 9, 9),
            currentMinute = 800,
        )

        assertEquals(ScheduleNextCourseKind.TODAY, result.kind)
        assertEquals("15:00", result.startTime)
    }

    @Test
    fun `next course returns tomorrow first after today is finished`() {
        val result = ScheduleCourseSelector.nextCourse(
            records = listOf(course(weekday = 4, start = 900, title = "Math")),
            today = LocalDate.of(2026, 9, 9),
            currentMinute = 1000,
        )

        assertEquals(ScheduleNextCourseKind.TOMORROW_FIRST, result.kind)
        assertEquals("15:00", result.startTime)
    }

    private fun course(
        weekday: Int,
        start: Int,
        title: String,
    ) = CalendarItem(
        weekday = weekday,
        startMin = start,
        endMin = start + 50,
        title = title,
    )
}
