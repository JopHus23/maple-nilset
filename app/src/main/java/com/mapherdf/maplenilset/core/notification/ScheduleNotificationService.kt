package com.mapherdf.maplenilset.core.notification

import android.app.Service
import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScheduleNotificationService : Service() {
    @Inject
    lateinit var coordinator: NotificationCoordinator

    @Inject
    lateinit var controller: NotificationController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        controller.createChannels()
        coordinator.setPersistentServiceRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startPersistentForeground(controller.buildPersistentServiceFallbackNotification())
        if (updateJob?.isActive != true) {
            updateJob = serviceScope.launch { updateLoop() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        coordinator.setPersistentServiceRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun updateLoop() {
        if (!controller.canNotify) {
            stopSelf()
            return
        }

        while (serviceScope.isActive) {
            val refreshState = coordinator.refresh()
            controller.updateReminderNotifications(refreshState.plan, refreshState.settings)
            val notification = refreshState.plan.currentOrNextCourse
                ?.let { controller.buildPersistentCourseNotification(it, refreshState.settings) }
                ?: controller.buildPersistentServiceFallbackNotification()
            startPersistentForeground(notification)
            delay(reminderPollIntervalMillis(refreshState.plan))
        }
    }

    private fun reminderPollIntervalMillis(plan: NotificationPlan): Long {
        val activeReminder = plan.activeReminders.minByOrNull { it.startAt }
        if (activeReminder != null) {
            val now = LocalDateTime.now()
            val secondsUntilStart = Duration.between(now, activeReminder.startAt).seconds
            if (secondsUntilStart > 0) {
                val displayedMinutes = ((secondsUntilStart + 59) / 60).toInt()
                val nextUpdateAt = activeReminder.startAt.minusMinutes(displayedMinutes - 1L)
                val millisUntilUpdate = Duration.between(now, nextUpdateAt).toMillis()
                if (millisUntilUpdate > 0) return millisUntilUpdate
            }
            return 1_000L
        }

        val nextReminderAt = plan.reminders
            .minByOrNull { it.remindAt }
            ?.remindAt
            ?: return 30_000L
        val millisUntilReminder = Duration
            .between(LocalDateTime.now(), nextReminderAt)
            .toMillis()
        return if (millisUntilReminder in 0..120_000L) 1_000L else 30_000L
    }

    private fun startPersistentForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationController.NOTIFICATION_ID_PERSISTENT_COURSE,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(
                NotificationController.NOTIFICATION_ID_PERSISTENT_COURSE,
                notification,
            )
        }
    }

    companion object {
        const val ACTION_STOP = "com.mapherdf.maplenilset.action.STOP_SCHEDULE_NOTIFICATION"
    }
}
