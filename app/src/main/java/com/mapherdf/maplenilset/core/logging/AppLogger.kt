package com.mapherdf.maplenilset.core.logging

import android.util.Log
import com.mapherdf.maplenilset.data.repository.ConsoleHistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger @Inject constructor(
    private val consoleHistoryRepository: ConsoleHistoryRepository,
    private val logFileWriter: LogFileWriter,
) {

    fun debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)

    fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)

    fun success(tag: String, message: String) = log(LogLevel.SUCCESS, tag, message)

    fun warning(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARNING, tag, message, throwable)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val consoleMessage = buildString {
            append("[$tag] ")
            append(message)
            throwable?.message
                ?.takeIf { it.isNotBlank() }
                ?.let { append(": ").append(it) }
        }

        val entry = ConsoleEntry(consoleMessage, level)
        consoleHistoryRepository.append(entry)
        logFileWriter.append(entry, throwable)

        val priority = when (level) {
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.INFO, LogLevel.SUCCESS -> Log.INFO
            LogLevel.WARNING -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
        }
        Log.println(priority, "Nilset/$tag", if (throwable != null) Log.getStackTraceString(throwable) else message)
    }
}
