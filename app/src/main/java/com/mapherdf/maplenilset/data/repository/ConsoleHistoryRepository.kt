package com.mapherdf.maplenilset.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.mapherdf.maplenilset.core.logging.ConsoleEntry
import javax.inject.Inject
import javax.inject.Singleton

/** 保存当前进程内的控制台输出，避免导航返回后历史丢失 */
@Singleton
class ConsoleHistoryRepository @Inject constructor() {

    private val _entries = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val entries: StateFlow<List<ConsoleEntry>> = _entries.asStateFlow()

    fun append(entry: ConsoleEntry) {
        _entries.update { entries ->
            (entries + entry).takeLast(MAX_ENTRY_COUNT)
        }
    }

    fun clear() {
        _entries.value = emptyList()
    }

    private companion object {
        const val MAX_ENTRY_COUNT = 500
    }
}
