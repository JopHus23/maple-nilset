package com.mapherdf.maplenilset.ui.schedule

import com.mapherdf.maplenilset.data.calendar.CalendarItem

object ScheduleCourseSelector {

    fun greetingFor(hour: Int): String = when (hour) {
        in 0..4 -> "凌晨好"
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..18 -> "下午好"
        else -> "晚上好"
    }

    fun coursesFor(records: List<CalendarItem>, weekday: Int): List<CalendarItem> {
        return records
            .filter { it.weekday == weekday }
            .sortedWith(compareBy({ it.startMin }, { it.endMin }, { it.title }))
    }

    fun nextCourse(
        records: List<CalendarItem>,
        today: java.time.LocalDate,
        currentMinute: Int,
    ): ScheduleNextCourse {
        val todayWeekday = today.dayOfWeek.value % 7
        val todayCourse = coursesFor(records, todayWeekday)
            .firstOrNull { it.startMin > currentMinute }
        if (todayCourse != null) {
            return ScheduleNextCourse(
                kind = ScheduleNextCourseKind.TODAY,
                title = todayCourse.title,
                startTime = formatTime(todayCourse.startMin),
                endTime = formatTime(todayCourse.endMin),
            )
        }

        if (records.isEmpty()) return ScheduleNextCourse()

        val tomorrowWeekday = today.plusDays(1).dayOfWeek.value % 7
        val tomorrowCourse = coursesFor(records, tomorrowWeekday).firstOrNull()
        if (tomorrowCourse == null) {
            return ScheduleNextCourse(kind = ScheduleNextCourseKind.TODAY_FINISHED)
        }
        return ScheduleNextCourse(
            kind = ScheduleNextCourseKind.TOMORROW_FIRST,
            title = tomorrowCourse.title,
            startTime = formatTime(tomorrowCourse.startMin),
            endTime = formatTime(tomorrowCourse.endMin),
        )
    }

    fun formatTime(minuteOfDay: Int): String {
        val normalizedMinute = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return "%02d:%02d".format(normalizedMinute / 60, normalizedMinute % 60)
    }
}

data class ScheduleCourseDraft(
    val title: String,
    val weekday: Int,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val teacher: String,
    val classroom: String,
    val note: String,
)

enum class ScheduleNextCourseKind {
    EMPTY,
    TODAY,
    TOMORROW_FIRST,
    TODAY_FINISHED,
}

data class ScheduleNextCourse(
    val kind: ScheduleNextCourseKind = ScheduleNextCourseKind.EMPTY,
    val title: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
)

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val records: List<CalendarItem> = emptyList(),
    val selectedWeekday: Int = 1,
    val selectedCourses: List<CalendarItem> = emptyList(),
    val nextCourse: ScheduleNextCourse = ScheduleNextCourse(),
    val greetingHour: Int = 12,
    val isCourseEditorVisible: Boolean = false,
    val editingCourse: CalendarItem? = null,
    val isSavingCourse: Boolean = false,
    val courseEditorError: String? = null,
    val errorMessage: String? = null,
)
