package com.mapherdf.maplenilset.core.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSettings(
    val scheduleNotificationsEnabled: Boolean = false,
    val persistentCourseEnabled: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderLeadMinutes: Int = DEFAULT_REMINDER_LEAD_MINUTES,
    val showClassroom: Boolean = true,
    val showTeacher: Boolean = false,
    val showNote: Boolean = false,
) {
    companion object {
        const val MIN_REMINDER_LEAD_MINUTES = 1
        const val MAX_REMINDER_LEAD_MINUTES = 10_080
        const val DEFAULT_REMINDER_LEAD_MINUTES = 10
    }
}
