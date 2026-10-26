package com.mapherdf.maplenilset.ui.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mapherdf.maplenilset.core.logging.ConsoleEntry
import com.mapherdf.maplenilset.core.logging.LogLevel
import com.mapherdf.maplenilset.core.command.CommandContext
import com.mapherdf.maplenilset.core.command.NilSetCommandCenter
import com.mapherdf.maplenilset.data.repository.ConsoleHistoryRepository
import com.mapherdf.maplenilset.data.repository.CalendarRepository
import javax.inject.Inject

/**
 * UI 层：控制台页 ViewModel。
 *
 * 职责：收集指令事件，交给核心层 [NilSetCommandCenter] 执行，把输出列表通过 [ConsoleUiState] 单向暴露给 UI。
 */
@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val consoleHistoryRepository: ConsoleHistoryRepository,
    private val calendarRepository: CalendarRepository,
    private val commandCenter: NilSetCommandCenter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConsoleUiState(consoleHistoryRepository.entries.value))
    val uiState: StateFlow<ConsoleUiState> = _uiState.asStateFlow()

    init {
        if (consoleHistoryRepository.entries.value.isEmpty()) {
            appendOutput("控制台已连接，输入 /help 查看可用指令。", LogLevel.INFO)
        }

        viewModelScope.launch {
            consoleHistoryRepository.entries.collect { entries ->
                _uiState.value = ConsoleUiState(entries)
            }
        }
    }

    /** 用户发送指令：交给核心层解析执行，结果追加进输出列表 */
    fun onCommandSent(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return

        val context = CommandContext(
            consoleHistoryRepository = consoleHistoryRepository,
            commandInput = trimmed,
            calendarRepository = calendarRepository,
        )
        val result = commandCenter.execute(trimmed, context)
        appendOutput("> $trimmed", LogLevel.INFO)
        appendOutput(result, LogLevel.INFO)
    }

    fun commandSuggestionsFor(
        input: String,
        cursor: Int,
    ): List<ConsoleCommandSuggestion> = commandCenter.complete(input, cursor).map { candidate ->
        ConsoleCommandSuggestion(
            displayText = candidate.displayText,
            description = candidate.description,
            replacementStart = candidate.replacementStart,
            replacementEnd = candidate.replacementEnd,
            appendSpace = candidate.appendSpace,
        )
    }

    private fun appendOutput(text: String, level: LogLevel) {
        consoleHistoryRepository.append(ConsoleEntry(message = text, level = level))
    }

}

/** 控制台 UI 状态：输出条目列表，由 UI 负责拼接渲染 */
data class ConsoleUiState(
    val entries: List<ConsoleEntry> = emptyList(),
)

data class ConsoleCommandSuggestion(
    val displayText: String,
    val description: String,
    val replacementStart: Int,
    val replacementEnd: Int,
    val appendSpace: Boolean,
)
