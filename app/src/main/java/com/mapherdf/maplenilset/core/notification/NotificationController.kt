package com.mapherdf.maplenilset.core.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mapherdf.maplenilset.MainActivity
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.data.calendar.CalendarItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmScheduler: NotificationAlarmScheduler,
    private val scheduleStore: NotificationScheduleStore,
) {
    private val channelsCreated = AtomicBoolean(false)
    private val displayedReminderMinutes = ConcurrentHashMap<Int, Int>()

    val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val canNotify: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    val canScheduleExactAlarms: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    fun createChannels() {
        if (channelsCreated.getAndSet(true)) return
        val persistent = NotificationChannel(
            CHANNEL_PERSISTENT_COURSE,
            context.getString(R.string.notification_channel_persistent),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_persistent_description)
            setShowBadge(false)
        }
        val reminder = NotificationChannel(
            CHANNEL_PRE_CLASS_REMINDER,
            context.getString(R.string.notification_channel_reminder),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_reminder_description)
        }
        notificationManager.createNotificationChannels(listOf(persistent, reminder))
    }

    suspend fun refresh(plan: NotificationPlan, settings: NotificationSettings) {
        createChannels()
        alarmScheduler.cancelExisting()
        if (!canNotify || !settings.scheduleNotificationsEnabled) {
            notificationManager.cancel(NOTIFICATION_ID_PERSISTENT_COURSE)
            return
        }

        if (settings.persistentCourseEnabled) {
            plan.currentOrNextCourse?.let { notificationManager.notify(NOTIFICATION_ID_PERSISTENT_COURSE, buildPersistentCourseNotification(it, settings)) }
            if (plan.currentOrNextCourse == null) {
                notificationManager.cancel(NOTIFICATION_ID_PERSISTENT_COURSE)
            }
        } else {
            notificationManager.cancel(NOTIFICATION_ID_PERSISTENT_COURSE)
        }

        val scheduledAlarms = buildList {
            if (settings.reminderEnabled) addAll(plan.reminders)
            if (settings.reminderEnabled) addAll(plan.reminderTicks)
            if (settings.persistentCourseEnabled) addAll(plan.persistentEvents)
        }
        if (scheduledAlarms.isNotEmpty()) {
            alarmScheduler.schedule(scheduledAlarms)
        }

        if (settings.reminderEnabled) {
            plan.activeReminders.forEach { postReminder(it, settings) }
        }

        plan.currentOrNextCourse
            ?.takeIf { !it.startAt.isAfter(LocalDateTime.now()) }
            ?.let { occurrence ->
                displayedReminderMinutes.remove(reminderNotificationId(occurrence))
                notificationManager.cancel(reminderNotificationId(occurrence))
            }
    }

    suspend fun postReminder(occurrence: NotificationOccurrence, settings: NotificationSettings) {
        createChannels()
        if (!canNotify) return
        val firedOnce = scheduleStore.markReminderFiredOnce(occurrence.reminderId)
        if (!firedOnce) return
        postOrUpdateReminder(occurrence, settings, silent = false)
    }

    fun updateReminder(occurrence: NotificationOccurrence, settings: NotificationSettings) {
        if (!canNotify || !settings.reminderEnabled) return
        createChannels()
        postOrUpdateReminder(occurrence, settings, silent = true)
    }

    fun updateReminderNotifications(plan: NotificationPlan, settings: NotificationSettings) {
        if (!canNotify || !settings.reminderEnabled) return
        createChannels()
        plan.activeReminders.forEach { occurrence ->
            val minutes = reminderMinutesRemaining(occurrence, settings)
            val notificationId = reminderNotificationId(occurrence)
            if (displayedReminderMinutes[notificationId] == minutes) return@forEach

            displayedReminderMinutes[notificationId] = minutes
            notificationManager.notify(notificationId, buildReminderNotification(occurrence, settings, minutes, silent = true))
        }
    }

    fun postTestReminder() {
        createChannels()
        if (!canNotify) return
        notificationManager.notify(
            TEST_NOTIFICATION_ID,
            baseBuilder(
                channelId = CHANNEL_PRE_CLASS_REMINDER,
                title = context.getString(R.string.notification_test_title),
                text = context.getString(R.string.notification_test_message),
                autoCancel = true,
            ).setCategory(NotificationCompat.CATEGORY_EVENT).build(),
        )
    }

    fun buildPersistentCourseNotification(
        occurrence: NotificationOccurrence,
        settings: NotificationSettings,
    ): Notification {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val now = LocalDateTime.now()
        val timeRange = "${occurrence.startAt.format(formatter)}-${occurrence.endAt.format(formatter)}"
        var contentText = when {
            now >= occurrence.startAt -> context.getString(
                R.string.notification_persistent_now,
                occurrence.course.title,
                timeRange,
            )
            else -> context.getString(
                R.string.notification_persistent_next,
                occurrence.course.title,
                timeRange,
            )
        }
        val externalClassroom = occurrence.course.classroom
            ?.takeIf { settings.showClassroom && it.isNotBlank() }
        if (externalClassroom != null) {
            contentText = context.getString(
                R.string.notification_persistent_classroom,
                contentText,
                externalClassroom,
            )
        }
        val notification = baseBuilder(
            channelId = CHANNEL_PERSISTENT_COURSE,
            title = context.getString(R.string.notification_persistent_title),
            text = contentText,
            autoCancel = false,
        )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildDetail(
                        occurrence,
                        contentText,
                        settings,
                        firstLineContainsCourseContext = true,
                    ),
                ),
            )
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        return notification
    }

    fun buildPersistentServiceFallbackNotification(): Notification {
        return baseBuilder(
            channelId = CHANNEL_PERSISTENT_COURSE,
            title = context.getString(R.string.notification_persistent_title),
            text = context.getString(R.string.notification_persistent_syncing),
            autoCancel = false,
        )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun baseBuilder(
        channelId: String,
        title: String,
        text: String,
        autoCancel: Boolean,
    ): NotificationCompat.Builder {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(autoCancel)
            .setShowWhen(true)
    }

    private fun buildDetail(
        occurrence: NotificationOccurrence,
        firstLine: String,
        settings: NotificationSettings,
        firstLineContainsCourseContext: Boolean = false,
    ): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val details = buildList {
            if (!firstLineContainsCourseContext) {
                add(
                    context.getString(
                        R.string.notification_course_time,
                        occurrence.startAt.format(formatter),
                        occurrence.endAt.format(formatter),
                    ),
                )
            }
            occurrence.course.classroom
                ?.takeIf { !firstLineContainsCourseContext && settings.showClassroom && it.isNotBlank() }
                ?.let {
                    add(context.getString(R.string.notification_course_classroom, it))
                }
            occurrence.course.teacher?.takeIf { settings.showTeacher && it.isNotBlank() }?.let {
                add(context.getString(R.string.notification_course_teacher, it))
            }
            occurrence.course.note?.takeIf { settings.showNote && it.isNotBlank() }?.let {
                add(context.getString(R.string.notification_course_note, it))
            }
        }
        return (listOf(firstLine) + details).joinToString("\n")
    }

    private fun buildReminderNotification(
        occurrence: NotificationOccurrence,
        settings: NotificationSettings,
        minutes: Int,
        silent: Boolean,
    ): Notification {
        val contentTitle = context.getString(
            R.string.notification_reminder_title,
            occurrence.course.title,
        )
        val contentText = context.getString(
            R.string.notification_reminder_in_minutes,
            minutes,
        )
        val builder = baseBuilder(
            channelId = CHANNEL_PRE_CLASS_REMINDER,
            title = contentTitle,
            text = contentText,
            autoCancel = true,
        )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    buildDetail(occurrence, contentText, settings),
                ),
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOnlyAlertOnce(true)

        if (silent) {
            return builder
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        }

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_FULL_SCREEN_REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return builder
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(fullScreenIntent, true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 220, 140, 220))
            .build()
    }

    private fun reminderMinutesRemaining(
        occurrence: NotificationOccurrence,
        settings: NotificationSettings,
    ): Int {
        val secondsUntilStart = Duration
            .between(LocalDateTime.now(), occurrence.startAt)
            .seconds
        val minutesUntilStart = ((secondsUntilStart + 59) / 60).toInt()
        return minutesUntilStart
            .coerceIn(0, settings.reminderLeadMinutes)
    }

    private fun reminderNotificationId(occurrence: NotificationOccurrence): Int {
        return REMINDER_NOTIFICATION_ID_BASE +
            (occurrence.reminderId and Int.MAX_VALUE) % REMINDER_NOTIFICATION_ID_RANGE
    }

    private fun postOrUpdateReminder(
        occurrence: NotificationOccurrence,
        settings: NotificationSettings,
        silent: Boolean,
    ) {
        val minutes = reminderMinutesRemaining(occurrence, settings)
        val notificationId = reminderNotificationId(occurrence)
        displayedReminderMinutes[notificationId] = minutes
        notificationManager.notify(
            notificationId,
            buildReminderNotification(occurrence, settings, minutes, silent = silent),
        )
    }

    companion object {
        const val CHANNEL_PERSISTENT_COURSE = "schedule_persistent_course"
        const val CHANNEL_PRE_CLASS_REMINDER = "schedule_pre_class_reminder"
        const val NOTIFICATION_ID_PERSISTENT_COURSE = 1001
        const val TEST_NOTIFICATION_ID = 1002
        const val REMINDER_NOTIFICATION_ID_BASE = 200_000
        const val REMINDER_NOTIFICATION_ID_RANGE = 800_000
        private const val NOTIFICATION_FULL_SCREEN_REQUEST_CODE = 1001
    }
}
