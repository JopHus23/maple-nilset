package com.mapherdf.maplenilset

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.mapherdf.maplenilset.core.logging.LogFileWriter
import javax.inject.Inject

@HiltAndroidApp
class MapleNilsetApplication : Application() {
    @Inject
    lateinit var logFileWriter: LogFileWriter

    override fun onCreate() {
        super.onCreate()
        logFileWriter.markSessionStarted()
    }
}
