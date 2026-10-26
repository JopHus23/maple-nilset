package com.mapherdf.maplenilset.core.command.commands

import com.mapherdf.maplenilset.core.command.CommandContext
import com.mapherdf.maplenilset.core.command.CommandResult
import com.mapherdf.maplenilset.core.command.NilSetCommand

/** 清空控制台历史输出 */
class ClearConsoleCommand : NilSetCommand {
    override val name = "cls"
    override val description = "清空控制台历史"

    override fun execute(context: CommandContext): CommandResult {
        context.consoleHistoryRepository.clear()
        return CommandResult.Success("控制台已清空")
    }
}
