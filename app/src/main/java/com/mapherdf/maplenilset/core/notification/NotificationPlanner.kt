package com.mapherdf.maplenilset.core.notification

import com.mapherdf.maplenilset.data.calendar.CalendarItem
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

data class NotificationOccurrence(
    val course: CalendarItem,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val remindAt: LocalDateTime,
    val alarmType: NotificationAlarmType = NotificationAlarmType.REMINDER,
    val triggerAt: LocalDateTime = remindAt,
) {
    val reminderId: Int
        get() = 31 * startAt.hashCode() + course.hashCode()

    val triggerId: Int
        get() = 31 * (
            31 * (startAt.hashCode() * 31 + course.hashCode()) + triggerAt.hashCode()
            ) + alarmType.hashCode()
}

data class NotificationPlan(
    val currentOrNextCourse: NotificationOccurrence?,
    val reminders: List<NotificationOccurrence>,
    val activeReminders: List<NotificationOccurrence> = emptyList(),
    val persistentEvents: List<NotificationOccurrence> = emptyList(),
    val reminderTicks: List<NotificationOccurrence> = emptyList(),
)

enum class NotificationAlarmType {
    REMINDER,
    REMINDER_TICK,
    COURSE_START,
    COURSE_END,
}

object NotificationPlanner {
    private const val MAX_REMINDERS = 48
    private const val MIN_SEARCH_DAYS = 8
    const val MAX_REMINDER_TICK_MINUTES = 30

    fun plan(
        records: List<CalendarItem>,
        settings: NotificationSettings,
        clock: Clock,
    ): NotificationPlan {
        val now = LocalDateTime.now(clock)
        val enabled = settings.scheduleNotificationsEnabled &&
            (settings.persistentCourseEnabled || settings.reminderEnabled)
        if (!enabled || records.isEmpty()) return NotificationPlan(null, emptyList())

        val searchDays = maxOf(
            MIN_SEARCH_DAYS,
            settings.reminderLeadMinutes / Duration.ofDays(1).toMinutes().toInt() + 2,
        )
        val reminders = mutableListOf<NotificationOccurrence>()
        val activeReminders = mutableListOf<NotificationOccurrence>()
        val reminderTicks = mutableListOf<NotificationOccurrence>()
        val upcomingOccurrences = mutableListOf<NotificationOccurrence>()

        for (dayOffset in 0 until searchDays) {
            val date = now.toLocalDate().plusDays(dayOffset.toLong())
            val weekday = date.dayOfWeek.value % 7
            records.filter { it.weekday == weekday }.forEach { course ->
                val startAt = date.atStartOfDay().plusMinutes(course.startMin.toLong())
                val endAt = date.atStartOfDay().plusMinutes(course.endMin.toLong())
                if (!endAt.isAfter(now)) return@forEach

                val occurrence = NotificationOccurrence(
                    course = course,
                    startAt = startAt,
                    endAt = endAt,
                    remindAt = startAt.minusMinutes(settings.reminderLeadMinutes.toLong()),
                )
                if (settings.reminderEnabled &&
                    !occurrence.remindAt.isAfter(now) &&
                    !occurrence.startAt.isBefore(now)
                ) {
                    activeReminders += occurrence
                    if (settings.reminderLeadMinutes <= MAX_REMINDER_TICK_MINUTES) {
                        val secondsUntilStart =
                            Duration.between(now, occurrence.startAt).seconds
                        val displayedMinutes =
                            ((secondsUntilStart + 59) / 60).toInt()
                        val nextTickAt = occurrence.startAt.minusMinutes(displayedMinutes - 1L)
                        if (displayedMinutes > 0 && nextTickAt.isAfter(now)) {
                            reminderTicks += occurrence.copy(
                                alarmType = NotificationAlarmType.REMINDER_TICK,
                                triggerAt = nextTickAt,
                            )
                        }
                    }
                } else if (
                    settings.reminderEnabled &&
                    occurrence.remindAt.isAfter(now) &&
                    !occurrence.startAt.isBefore(now)
                ) {
                    reminders += occurrence
                }
                upcomingOccurrences += occurrence
            }
        }

        val currentOrNextCourse = upcomingOccurrences.minByOrNull { it.startAt }
        val persistentEvents = if (settings.persistentCourseEnabled) {
            buildList {
                currentOrNextCourse?.let { occurrence ->
                    if (occurrence.startAt.isAfter(now)) {
                        add(
                            occurrence.copy(
                                alarmType = NotificationAlarmType.COURSE_START,
                                triggerAt = occurrence.startAt,
                            ),
                        )
                    }
                    add(
                        occurrence.copy(
                            alarmType = NotificationAlarmType.COURSE_END,
                            triggerAt = occurrence.endAt,
                        ),
                    )
                }
            }
        } else {
            emptyList()
        }

        return NotificationPlan(
            currentOrNextCourse = currentOrNextCourse,
            reminders = reminders
                .distinctBy { it.triggerId }
                .sortedBy { it.remindAt }
                .take(MAX_REMINDERS),
            activeReminders = activeReminders
                .distinctBy { it.triggerId }
                .sortedBy { it.remindAt },
            persistentEvents = persistentEvents.distinctBy { it.triggerId },
            reminderTicks = reminderTicks.distinctBy { it.triggerId },
        )
    }
}
