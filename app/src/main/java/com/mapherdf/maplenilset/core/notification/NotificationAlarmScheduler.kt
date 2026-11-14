package com.mapherdf.maplenilset.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mapherdf.maplenilset.MainActivity
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleStore: NotificationScheduleStore,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun cancelExisting() {
        scheduleStore.readIds().forEach { id ->
            pendingIntent(id, flags = PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
        }
        scheduleStore.saveIds(emptyList())
    }

    fun scheduleNextReminderTick(
        occurrence: NotificationOccurrence,
        settings: NotificationSettings,
        clock: java.time.Clock,
    ) {
        if (!settings.reminderEnabled ||
            settings.reminderLeadMinutes > NotificationPlanner.MAX_REMINDER_TICK_MINUTES
        ) return

        val now = LocalDateTime.now(clock)
        val secondsUntilStart = Duration.between(now, occurrence.startAt).seconds
        if (secondsUntilStart <= 0) return

        val displayedMinutes = ((secondsUntilStart + 59) / 60).toInt()
        val nextTickAt = occurrence.startAt.minusMinutes(displayedMinutes - 1L)
        if (displayedMinutes <= 0 || !nextTickAt.isAfter(now)) return

        val tickOccurrence = occurrence.copy(
            alarmType = NotificationAlarmType.REMINDER_TICK,
            triggerAt = nextTickAt,
        )
        val id = tickOccurrence.triggerId and Int.MAX_VALUE
        val intent = pendingIntent(
            id = id,
            flags = PendingIntent.FLAG_UPDATE_CURRENT,
            occurrence = tickOccurrence,
        ) ?: return
        val triggerAtMillis = nextTickAt
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val showIntent = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager?.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
            intent,
        )
    }

    suspend fun schedule(occurrences: List<NotificationOccurrence>) {
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager?.canScheduleExactAlarms() == true
        val ids = occurrences.map { occurrence ->
            val id = occurrence.triggerId and Int.MAX_VALUE
            val intent = pendingIntent(
                id = id,
                flags = PendingIntent.FLAG_UPDATE_CURRENT,
                occurrence = occurrence,
            ) ?: return@map id
            val triggerAtMillis = occurrence.triggerAt
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            if (occurrence.alarmType == NotificationAlarmType.REMINDER ||
                occurrence.alarmType == NotificationAlarmType.REMINDER_TICK
            ) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    id,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager?.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                    intent,
                )
            } else if (canUseExact) {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intent,
                )
            } else {
                alarmManager?.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intent,
                )
            }
            id
        }
        scheduleStore.saveIds(ids)
    }

    private fun pendingIntent(
        id: Int,
        flags: Int,
        occurrence: NotificationOccurrence? = null,
    ): PendingIntent? {
        val immutable = PendingIntent.FLAG_IMMUTABLE
        val finalFlags = if (flags == PendingIntent.FLAG_NO_CREATE) {
            flags or immutable
        } else {
            flags or immutable
        }
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        occurrence?.let {
            intent.putExtra(EXTRA_TRIGGER_ID, id)
            intent.putExtra(EXTRA_ALARM_TYPE, it.alarmType.name)
            intent.putExtra(EXTRA_WEEKDAY, it.course.weekday)
            intent.putExtra(EXTRA_START_MIN, it.course.startMin)
            intent.putExtra(EXTRA_END_MIN, it.course.endMin)
            intent.putExtra(EXTRA_TITLE, it.course.title)
            intent.putExtra(EXTRA_TEACHER, it.course.teacher)
            intent.putExtra(EXTRA_CLASSROOM, it.course.classroom)
            intent.putExtra(EXTRA_NOTE, it.course.note)
            intent.putExtra(
                EXTRA_START_EPOCH_SECOND,
                it.startAt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
            )
            intent.putExtra(
                EXTRA_END_EPOCH_SECOND,
                it.endAt.atZone(java.time.ZoneId.systemDefault()).toEpochSecond(),
            )
        }
        return PendingIntent.getBroadcast(context, id, intent, finalFlags)
    }

    companion object {
        const val EXTRA_TRIGGER_ID = "trigger_id"
        const val EXTRA_ALARM_TYPE = "alarm_type"
        const val EXTRA_WEEKDAY = "weekday"
        const val EXTRA_START_MIN = "start_min"
        const val EXTRA_END_MIN = "end_min"
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEACHER = "teacher"
        const val EXTRA_CLASSROOM = "classroom"
        const val EXTRA_NOTE = "note"
        const val EXTRA_START_EPOCH_SECOND = "start_epoch_second"
        const val EXTRA_END_EPOCH_SECOND = "end_epoch_second"
    }
}
