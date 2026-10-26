package com.mapherdf.maplenilset.core.command

/**
 * 指令接口：每条控制台指令实现一个实例，注册进 [CommandRegistry]。
 * 新增指令只需实现此接口，不改分发器。
 */
interface NilSetCommand {

    /** 指令名，不含 / 前缀，如 "no:login" */
    val name: String

    /** 展示在 /help 里的说明 */
    val description: String

    /** 是否仅 debug 可用（release 包中完全隐藏 + 拒绝执行） */
    val isDebugOnly: Boolean
        get() = false

    fun completeArgument(context: CommandCompletionContext): List<CommandCompletion> = emptyList()

    fun execute(context: CommandContext): CommandResult
}
