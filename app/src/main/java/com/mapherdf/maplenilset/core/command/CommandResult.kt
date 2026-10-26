package com.mapherdf.maplenilset.core.command

/** 指令执行的结构化结果，UI 负责渲染 */
sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class Failure(val message: String) : CommandResult()
}
