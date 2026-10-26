package com.mapherdf.maplenilset.core.command

/**
 * 指令注册表：收集所有 [NilSetCommand]，提供查找和可见性过滤。
 * debug 指令在 release 构建中完全不可见且不可执行。
 */
class CommandRegistry(
    commands: List<NilSetCommand>,
    val isDebug: Boolean,
) {
    private val commandMap = commands.associateBy { it.name }
    private val allCommands = commands.toList()

    fun find(name: String): NilSetCommand? = commandMap[name]

    /** 返回当前构建类型下可见的指令列表 */
    fun visibleCommands(): List<NilSetCommand> =
        allCommands.filter { isDebug || !it.isDebugOnly }
}
