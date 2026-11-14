package com.mapherdf.maplenilset.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import com.mapherdf.maplenilset.di.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationAlarmReceiver : BroadcastReceiver() {
    @Inject
    lateinit var coordinator: NotificationCoordinator

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                val extras = intent.extras ?: android.os.Bundle()
                val alarmType = runCatching {
                    NotificationAlarmType.valueOf(
                        extras.getString(NotificationAlarmScheduler.EXTRA_ALARM_TYPE).orEmpty(),
                    )
                }.getOrDefault(NotificationAlarmType.REMINDER)
                if (alarmType == NotificationAlarmType.REMINDER ||
                    alarmType == NotificationAlarmType.REMINDER_TICK
                ) {
                    coordinator.onReminderAlarm(alarmType, extras)
                } else {
                    coordinator.refresh()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
