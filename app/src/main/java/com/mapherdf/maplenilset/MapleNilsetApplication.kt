package com.mapherdf.maplenilset

import android.app.Application
import com.mapherdf.maplenilset.core.notification.NotificationCoordinator
import dagger.hilt.android.HiltAndroidApp
import com.mapherdf.maplenilset.core.logging.LogFileWriter
import javax.inject.Inject

@HiltAndroidApp
class MapleNilsetApplication : Application() {
    @Inject
    lateinit var logFileWriter: LogFileWriter

    @Inject
    lateinit var notificationCoordinator: NotificationCoordinator

    override fun onCreate() {
        super.onCreate()
        logFileWriter.markSessionStarted()
        notificationCoordinator.initialize()
    }
}
