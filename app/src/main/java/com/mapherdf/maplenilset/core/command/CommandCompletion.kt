package com.mapherdf.maplenilset.core.command

data class CommandCompletion(
    val value: String,
    val description: String,
    val appendSpace: Boolean = true,
)

data class CommandCompletionContext(
    val commandName: String,
    val completedArguments: List<String>,
    val currentPrefix: String,
    val hasCurrentArgument: Boolean,
)

data class CommandCompletionCandidate(
    val displayText: String,
    val description: String,
    val replacementStart: Int,
    val replacementEnd: Int,
    val appendSpace: Boolean,
)

data class ParsedCommandInput(
    val commandName: String,
    val commandNameRange: IntRange,
    val arguments: List<String>,
    val currentPrefix: String?,
    val currentArgumentRange: IntRange?,
    val isCompletingCommandName: Boolean,
) {
    val hasCurrentArgument: Boolean
        get() = currentPrefix != null

    fun toCompletionContext(): CommandCompletionContext = CommandCompletionContext(
        commandName = commandName,
        completedArguments = arguments,
        currentPrefix = currentPrefix.orEmpty(),
        hasCurrentArgument = hasCurrentArgument,
    )
}

internal object CommandInputParser {
    fun parse(input: String, cursor: Int): ParsedCommandInput? {
        val safeCursor = cursor.coerceIn(0, input.length)
        val tokens = mutableListOf<Token>()
        val bodyStart = if (input.startsWith("/")) 1 else 0
        var index = bodyStart

        while (index < input.length) {
            while (index < input.length && input[index].isWhitespace()) {
                index++
            }
            if (index >= input.length) {
                break
            }

            val tokenStart = index
            val content = StringBuilder()
            var quote: Char? = null
            if (input[index] == '"' || input[index] == '\'') {
                quote = input[index]
                index++
            }

            while (index < input.length) {
                val current = input[index]
                when {
                    quote != null && current == quote -> {
                        index++
                        break
                    }

                    quote == null && current.isWhitespace() -> break

                    else -> {
                        content.append(current)
                        index++
                    }
                }
            }

            val tokenEnd = index
            val token = Token(
                text = content.toString(),
                range = tokenStart until tokenEnd,
            )
            if (safeCursor <= tokenEnd) {
                return (tokens + token).toParsedCommandInput(input, token)
            }
            tokens += token
        }

        return tokens.toParsedCommandInput(input, null)
    }

    private fun List<Token>.toParsedCommandInput(
        input: String,
        currentToken: Token?,
    ): ParsedCommandInput? {
        if (isEmpty()) {
            return null
        }

        val command = first()
        val completedArguments = filter { token -> token !== currentToken }
            .drop(1)
            .map(Token::text)
        val isCompletingCommandName = currentToken != null && currentToken == first()
        val commandStart = command.range.first
        val slashPresent = commandStart > 0 && input[commandStart - 1] == '/'

        return ParsedCommandInput(
            commandName = command.text,
            commandNameRange = if (slashPresent) {
                (commandStart - 1)..command.range.last
            } else {
                command.range
            },
            arguments = completedArguments,
            currentPrefix = currentToken?.text,
            currentArgumentRange = currentToken?.range,
            isCompletingCommandName = isCompletingCommandName,
        )
    }

    private data class Token(
        val text: String,
        val range: IntRange,
    )
}
