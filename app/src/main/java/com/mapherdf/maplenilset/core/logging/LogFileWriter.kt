package com.mapherdf.maplenilset.core.logging

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.mapherdf.maplenilset.const.LogFiles
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogFileWriter @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val startedAt = LocalDateTime.now()
    private val fileTimestamp = startedAt.format(fileNameFormatter)
    private val appendLock = Any()
    val sessionFile: File = createSessionFile(context)

    fun markSessionStarted() {
        synchronized(appendLock) {
            sessionFile.appendText(
                buildString {
                    appendLine("=== Nilset app session ===")
                    appendLine("Started: ${startedAt.format(fileLineFormatter)}")
                    appendLine()
                }
            )
        }
    }

    fun append(entry: ConsoleEntry, throwable: Throwable? = null) {
        synchronized(appendLock) {
            sessionFile.appendText(
                buildString {
                    append(entry.displayText())
                    appendLine()
                    throwable?.let {
                        append(Log.getStackTraceString(it))
                        appendLine()
                    }
                }
            )
        }
    }

    private fun createSessionFile(context: Context): File {
        val directory = File(context.filesDir, LogFiles.DIRECTORY).apply { mkdirs() }
        val nextIndex = directory.listFiles { file -> file.isFile }
            .orEmpty()
            .maxOfOrNull { it.nextSessionIndex() }
            ?: 1

        val fileName = buildString {
            append(LogFiles.FILE_PREFIX)
            append('-')
            append(nextIndex.toString().padStart(SESSION_INDEX_WIDTH, '0'))
            append('-')
            append(fileTimestamp)
            append('.')
            append(LogFiles.FILE_EXTENSION)
        }
        return File(directory, fileName)
    }

    private fun File.nextSessionIndex(): Int {
        val fileExtension = ".${LogFiles.FILE_EXTENSION}"
        val fileNamePrefix = "${LogFiles.FILE_PREFIX}-"
        if (!name.startsWith(fileNamePrefix) || !name.endsWith(fileExtension)) return 0

        val withoutPrefix = name.removePrefix(fileNamePrefix)
        val index = withoutPrefix.substringBefore('-').toIntOrNull() ?: return 0
        return index + 1
    }

    private companion object {
        const val SESSION_INDEX_WIDTH = 6
        val fileNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val fileLineFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}
