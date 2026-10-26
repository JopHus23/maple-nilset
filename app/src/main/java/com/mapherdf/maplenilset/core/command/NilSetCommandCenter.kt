package com.mapherdf.maplenilset.core.command

import com.mapherdf.maplenilset.const.CommandExpressions

/**
 * 控制台指令分发中心（纯 Kotlin，不依赖 Android UI）。
 * 接收用户输入，通过 [CommandRegistry] 查找并执行对应指令。
 * 新增指令只需实现 [NilSetCommand] 并注册，不改此类。
 */
class NilSetCommandCenter(private val registry: CommandRegistry) {

    fun complete(input: String, cursor: Int): List<CommandCompletionCandidate> {
        if (!input.startsWith(CommandExpressions.PREFIX)) {
            return emptyList()
        }

        if (input.removePrefix(CommandExpressions.PREFIX).isBlank()) {
            return commandNameCandidates(
                prefix = "",
                replacementStart = 0,
                replacementEnd = input.length,
            )
        }

        val parsed = CommandInputParser.parse(input, cursor) ?: return emptyList()
        if (parsed.isCompletingCommandName) {
            return commandNameCandidates(
                prefix = parsed.currentPrefix.orEmpty(),
                replacementStart = parsed.commandNameRange.first,
                replacementEnd = parsed.commandNameRange.last + 1,
            )
        }

        val command = registry.find(parsed.commandName)
            ?: return emptyList()
        if (command.isDebugOnly && !registry.isDebug) {
            return emptyList()
        }

        return command.completeArgument(parsed.toCompletionContext())
            .filter { completion ->
                completion.value.startsWith(parsed.currentPrefix.orEmpty(), ignoreCase = true)
            }
            .map { completion ->
                CommandCompletionCandidate(
                    displayText = completion.value,
                    description = completion.description,
                    replacementStart = parsed.currentArgumentRange?.first ?: input.length,
                    replacementEnd = (parsed.currentArgumentRange?.last ?: input.length - 1) + 1,
                    appendSpace = completion.appendSpace,
                )
            }
    }

    private fun dispatch(input: String, context: CommandContext): String {
        val command = input.trim()

        if (!command.startsWith(CommandExpressions.PREFIX)) {
            return "不是内部指令，请输入以 / 开头的指令（如 /help）"
        }

        val commandBody = command.removePrefix(CommandExpressions.PREFIX)
        val commandName = commandBody.takeWhile { !it.isWhitespace() }

        if (commandName == CommandExpressions.HELP) {
            return buildHelp()
        }

        val found = registry.find(commandName)
            ?: return "未知指令：$command（输入 /help 查看帮助）"

        if (found.isDebugOnly && !registry.isDebug) {
            return "未知指令：$command（输入 /help 查看帮助）"
        }

        return when (val result = found.execute(context)) {
            is CommandResult.Success -> result.message
            is CommandResult.Failure -> "错误：${result.message}"
        }
    }

    fun execute(input: String, context: CommandContext): String =
        dispatch(input, context)

    /** 返回当前构建可用的指令，供输入补全展示 */
    fun visibleCommands(): List<NilSetCommand> = registry.visibleCommands()

    private fun buildHelp(): String = buildString {
        appendLine("可用指令：")
        registry.visibleCommands().forEach { cmd ->
            appendLine("  /${cmd.name}   ${cmd.description}")
        }
    }.trimEnd()

    private fun commandNameCandidates(
        prefix: String,
        replacementStart: Int,
        replacementEnd: Int,
    ): List<CommandCompletionCandidate> {
        val candidates = buildList {
            add(
                CommandCompletion(
                    value = CommandExpressions.HELP,
                    description = "查看可用指令",
                ),
            )
            addAll(registry.visibleCommands().map { command ->
                CommandCompletion(
                    value = command.name,
                    description = command.description,
                )
            })
        }

        return candidates
            .filter { it.value.startsWith(prefix, ignoreCase = true) }
            .sortedBy { it.value.lowercase() }
            .map { completion ->
                CommandCompletionCandidate(
                    displayText = "${CommandExpressions.PREFIX}${completion.value}",
                    description = completion.description,
                    replacementStart = replacementStart,
                    replacementEnd = replacementEnd,
                    appendSpace = completion.appendSpace,
                )
            }
    }

}
