package com.mapherdf.maplenilset.core.command.commands

import com.mapherdf.maplenilset.core.command.CommandContext
import com.mapherdf.maplenilset.core.command.CommandResult
import com.mapherdf.maplenilset.core.command.NilSetCommand
import kotlinx.coroutines.runBlocking

/** 清空本地课表数据，仅 debug 构建可用 */
class ClearDataCommand : NilSetCommand {
    override val name = "clear:data"
    override val description = "清除本地课表"
    override val isDebugOnly = true

    override fun execute(context: CommandContext): CommandResult {
        runBlocking {
            context.calendarRepository.deleteCalendar()
        }
        return CommandResult.Success("已清除本地课表")
    }
}
