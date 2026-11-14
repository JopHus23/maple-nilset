package com.mapherdf.maplenilset.data.transfer

import com.mapherdf.maplenilset.data.calendar.CalendarItem
import com.mapherdf.maplenilset.data.color.SavedColor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class PageTransferCodecTest {

    @Test
    fun `schedule transfer encodes typed json and round trips`() {
        val records = listOf(
            CalendarItem(
                weekday = 1,
                startMin = 480,
                endMin = 540,
                title = "Math",
                teacher = "Teacher",
                classroom = "A101",
                note = null,
            ),
        )

        val encoded = SchedulePageTransferCodec.encode(records)
        val root = Json.parseToJsonElement(encoded).jsonObject

        assertEquals("maplenilset.schedule", root.getValue("type").jsonPrimitive.content)
        assertEquals(records, SchedulePageTransferCodec.decode(encoded))
    }

    @Test
    fun `schedule transfer rejects invalid course`() {
        val invalidJson = """
            {
              "type": "maplenilset.schedule",
              "version": 1,
              "records": [
                {"weekday": 1, "startMin": 540, "endMin": 480, "title": "Math"}
              ]
            }
        """.trimIndent()

        assertInvalidTransfer {
            SchedulePageTransferCodec.decode(invalidJson)
        }
    }

    @Test
    fun `schedule transfer rejects wrong type`() {
        val wrongJson = """
            {
              "type": "maplenilset.lienzo-color",
              "version": 1,
              "records": []
            }
        """.trimIndent()

        assertInvalidTransfer {
            SchedulePageTransferCodec.decode(wrongJson)
        }
    }

    @Test
    fun `color transfer encodes typed json and round trips`() {
        val colors = listOf(
            SavedColor(
                id = "color-1",
                name = "Red",
                hex = "#FF0000",
                sourceType = "HEX",
                sourceValue = "#FF0000",
                createdAt = 100L,
            ),
        )

        val encoded = ColorPageTransferCodec.encode(colors)
        val root = Json.parseToJsonElement(encoded).jsonObject

        assertEquals("maplenilset.lienzo-color", root.getValue("type").jsonPrimitive.content)
        assertEquals(colors, ColorPageTransferCodec.decode(encoded))
    }

    @Test
    fun `color transfer rejects duplicate ids and invalid hex`() {
        val color = SavedColor(
            id = "color-1",
            name = "Red",
            hex = "#FF0000",
            sourceType = "HEX",
            sourceValue = "#FF0000",
            createdAt = 100L,
        )
        val duplicateJson = ColorPageTransferCodec.encode(listOf(color, color))
        val invalidJson = ColorPageTransferCodec.encode(listOf(color.copy(hex = "#FF00")))

        assertInvalidTransfer {
            ColorPageTransferCodec.decode(duplicateJson)
        }
        assertInvalidTransfer {
            ColorPageTransferCodec.decode(invalidJson)
        }
    }

    private fun assertInvalidTransfer(block: () -> Unit) {
        try {
            block()
        } catch (error: IllegalArgumentException) {
            return
        }
        fail("Expected IllegalArgumentException")
    }
}
