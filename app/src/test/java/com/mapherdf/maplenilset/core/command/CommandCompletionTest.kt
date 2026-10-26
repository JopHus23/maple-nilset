package com.mapherdf.maplenilset.core.command

import com.mapherdf.maplenilset.core.command.commands.ClearConsoleCommand
import com.mapherdf.maplenilset.core.command.commands.ClearDataCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandCompletionTest {
    private val commandCenter = NilSetCommandCenter(
        CommandRegistry(
            commands = listOf(ClearDataCommand(), ClearConsoleCommand()),
            isDebug = true,
        ),
    )

    @Test
    fun `command name completion filters current prefix`() {
        val candidates = commandCenter.complete("/cl", cursor = 3)

        assertEquals(
            listOf("/clear:data", "/cls"),
            candidates.map { it.displayText },
        )
    }

    @Test
    fun `command name completes when whole command is typed`() {
        val candidates = commandCenter.complete("/cls", cursor = 4)

        assertEquals(listOf("/cls"), candidates.map { it.displayText })
    }

    @Test
    fun `slash shows all visible commands sorted by name`() {
        val candidates = commandCenter.complete("/", cursor = 1)

        assertEquals(
            listOf("/clear:data", "/cls", "/help"),
            candidates.map { it.displayText },
        )
        assertEquals(0, candidates.first().replacementStart)
        assertEquals(1, candidates.first().replacementEnd)
    }

    @Test
    fun `command without arguments has no next segment candidates`() {
        val candidates = commandCenter.complete("/cls ", cursor = 5)

        assertTrue(candidates.isEmpty())
    }
}
