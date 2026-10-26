package com.mapherdf.maplenilset.core.command

import com.mapherdf.maplenilset.data.repository.ConsoleHistoryRepository
import com.mapherdf.maplenilset.data.repository.CalendarRepository

/** 指令执行上下文：向指令暴露其所需的依赖 */
data class CommandContext(
    val consoleHistoryRepository: ConsoleHistoryRepository,
    val commandInput: String,
    val calendarRepository: CalendarRepository,
)
