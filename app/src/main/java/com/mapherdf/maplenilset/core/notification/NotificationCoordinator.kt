package com.mapherdf.maplenilset.core.notification

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import com.mapherdf.maplenilset.data.calendar.CalendarItem
import com.mapherdf.maplenilset.data.repository.CalendarRepository
import com.mapherdf.maplenilset.data.repository.NotificationSettingsRepository
import com.mapherdf.maplenilset.di.ApplicationScope
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class NotificationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val calendarRepository: CalendarRepository,
    private val settingsRepository: NotificationSettingsRepository,
    private val controller: NotificationController,
    private val alarmScheduler: NotificationAlarmScheduler,
    private val clock: Clock,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val refreshMutex = Mutex()
    private val persistentServiceRunning = AtomicBoolean(false)

    fun initialize() {
        controller.createChannels()
        settingsRepository.settings
            .onEach { refresh() }
            .launchIn(applicationScope)
    }

    suspend fun refresh(): NotificationRefreshState {
        refreshMutex.withLock {
            val settings = settingsRepository.settings.first()
            val records = calendarRepository.getCalendar().fold(
                onSuccess = { it.records },
                onFailure = { emptyList() },
            )
            val plan = NotificationPlanner.plan(records, settings, clock)
            controller.refresh(plan, settings)
            if (settings.reminderEnabled) {
                controller.updateReminderNotifications(plan, settings)
            }
            updatePersistentService(settings, plan.currentOrNextCourse != null)
            return NotificationRefreshState(plan, settings)
        }
    }

    fun setPersistentServiceRunning(running: Boolean) {
        persistentServiceRunning.set(running)
    }

    private fun updatePersistentService(
        settings: NotificationSettings,
        hasCurrentOrNextCourse: Boolean,
    ) {
        val shouldRun = settings.scheduleNotificationsEnabled &&
            settings.persistentCourseEnabled &&
            controller.canNotify &&
            hasCurrentOrNextCourse
        if (shouldRun && !persistentServiceRunning.get()) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ScheduleNotificationService::class.java),
            )
        } else if (!shouldRun && persistentServiceRunning.get()) {
            context.stopService(Intent(context, ScheduleNotificationService::class.java))
        }
    }

    data class NotificationRefreshState(
        val plan: NotificationPlan,
        val settings: NotificationSettings,
    )

    fun refreshAsync() {
        applicationScope.launch { refresh() }
    }

    fun postTestReminder() {
        controller.postTestReminder()
    }

    suspend fun onReminderAlarm(alarmType: NotificationAlarmType, extras: android.os.Bundle) {
        val course = CalendarItem(
            weekday = extras.getInt(NotificationAlarmScheduler.EXTRA_WEEKDAY),
            startMin = extras.getInt(NotificationAlarmScheduler.EXTRA_START_MIN),
            endMin = extras.getInt(NotificationAlarmScheduler.EXTRA_END_MIN),
            title = extras.getString(NotificationAlarmScheduler.EXTRA_TITLE).orEmpty(),
            teacher = extras.getString(NotificationAlarmScheduler.EXTRA_TEACHER),
            classroom = extras.getString(NotificationAlarmScheduler.EXTRA_CLASSROOM),
            note = extras.getString(NotificationAlarmScheduler.EXTRA_NOTE),
        )
        val zone = ZoneId.systemDefault()
        val startAt = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(extras.getLong(NotificationAlarmScheduler.EXTRA_START_EPOCH_SECOND)),
            zone,
        )
        val endAt = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(extras.getLong(NotificationAlarmScheduler.EXTRA_END_EPOCH_SECOND)),
            zone,
        )
        val settings = settingsRepository.settings.first()
        val remindAt = startAt.minusMinutes(settings.reminderLeadMinutes.toLong())
        val occurrence = NotificationOccurrence(
            course = course,
            startAt = startAt,
            endAt = endAt,
            remindAt = remindAt,
        )
        if (alarmType == NotificationAlarmType.REMINDER) {
            controller.postReminder(occurrence, settings)
        } else {
            controller.updateReminder(occurrence, settings)
        }
        alarmScheduler.scheduleNextReminderTick(occurrence, settings, clock)
        refreshAsync()
    }
}
