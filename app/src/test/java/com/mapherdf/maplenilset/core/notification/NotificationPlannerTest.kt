package com.mapherdf.maplenilset.core.notification

import com.mapherdf.maplenilset.data.calendar.CalendarItem
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationPlannerTest {

    @Test
    fun `plan returns next course reminder and persistent transition events`() {
        val clock = clockAt("2026-09-06T01:00:00Z")
        val settings = NotificationSettings(
            scheduleNotificationsEnabled = true,
            persistentCourseEnabled = true,
            reminderEnabled = true,
            reminderLeadMinutes = 10,
        )

        val plan = NotificationPlanner.plan(
            records = listOf(sundayCourse()),
            settings = settings,
            clock = clock,
        )

        assertEquals("Math", plan.currentOrNextCourse?.course?.title)
        val currentCourseReminders = plan.reminders.filter { it.startAt.dayOfMonth == 6 }
        assertEquals(1, currentCourseReminders.size)
        assertEquals(NotificationAlarmType.REMINDER, currentCourseReminders.single().alarmType)
        assertEquals(9, currentCourseReminders.single().remindAt.hour)
        assertEquals(
            listOf(
                NotificationAlarmType.COURSE_START,
                NotificationAlarmType.COURSE_END,
            ),
            plan.persistentEvents.map { it.alarmType },
        )
    }

    @Test
    fun `plan ignores stale reminder but keeps current course for persistent notification`() {
        val clock = clockAt("2026-09-06T02:00:00Z")
        val settings = NotificationSettings(
            scheduleNotificationsEnabled = true,
            persistentCourseEnabled = true,
            reminderEnabled = true,
            reminderLeadMinutes = 10,
        )

        val plan = NotificationPlanner.plan(
            records = listOf(sundayCourse()),
            settings = settings,
            clock = clock,
        )

        assertEquals(0, plan.reminders.filter { it.startAt.dayOfMonth == 6 }.size)
        assertEquals(1, plan.activeReminders.filter { it.startAt.dayOfMonth == 6 }.size)
        assertEquals(0, plan.reminderTicks.filter { it.startAt.dayOfMonth == 6 }.size)
        assertEquals("Math", plan.currentOrNextCourse?.course?.title)
        assertEquals(
            listOf(NotificationAlarmType.COURSE_END),
            plan.persistentEvents.map { it.alarmType },
        )
    }

    @Test
    fun `plan returns empty when notifications disabled`() {
        val plan = NotificationPlanner.plan(
            records = listOf(sundayCourse()),
            settings = NotificationSettings(),
            clock = clockAt("2026-09-06T01:00:00Z"),
        )

        assertNull(plan.currentOrNextCourse)
        assertEquals(0, plan.reminders.size)
        assertEquals(0, plan.reminderTicks.size)
        assertEquals(0, plan.persistentEvents.size)
    }

    @Test
    fun `plan schedules next reminder tick on minute boundary`() {
        val clock = clockAt("2026-09-06T01:50:20Z")
        val settings = NotificationSettings(
            scheduleNotificationsEnabled = true,
            reminderEnabled = true,
            reminderLeadMinutes = 10,
        )

        val plan = NotificationPlanner.plan(
            records = listOf(sundayCourse()),
            settings = settings,
            clock = clock,
        )

        assertEquals(LocalDateTime.parse("2026-09-06T09:51:00"), plan.reminderTicks.single().triggerAt)
    }

    @Test
    fun `plan selects current course instead of later unsorted course`() {
        val clock = clockAt("2026-09-06T01:00:00Z")
        val settings = NotificationSettings(
            scheduleNotificationsEnabled = true,
            persistentCourseEnabled = true,
            reminderEnabled = true,
            reminderLeadMinutes = 10,
        )

        val plan = NotificationPlanner.plan(
            records = listOf(
                sundayCourse(title = "Physics", startMin = 660, endMin = 710),
                sundayCourse(),
            ),
            settings = settings,
            clock = clock,
        )

        assertEquals("Math", plan.currentOrNextCourse?.course?.title)
        assertEquals(
            listOf(
                NotificationAlarmType.COURSE_START,
                NotificationAlarmType.COURSE_END,
            ),
            plan.persistentEvents.map { it.alarmType },
        )
    }

    private fun sundayCourse(
        title: String = "Math",
        startMin: Int = 600,
        endMin: Int = 650,
    ) = CalendarItem(
        weekday = 0,
        startMin = startMin,
        endMin = endMin,
        title = title,
        classroom = "A101",
    )

    private fun clockAt(time: String): Clock =
        Clock.fixed(Instant.parse(time), ZoneId.of("Asia/Shanghai"))
}
