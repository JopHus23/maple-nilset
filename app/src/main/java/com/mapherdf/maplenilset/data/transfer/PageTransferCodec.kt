package com.mapherdf.maplenilset.data.transfer

import com.mapherdf.maplenilset.data.calendar.CalendarItem
import com.mapherdf.maplenilset.data.color.ColorConverter
import com.mapherdf.maplenilset.data.color.SavedColor
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

enum class PageTransferMode {
    OVERWRITE,
    MERGE,
}

@Serializable
data class SchedulePageTransfer(
    val type: String = SCHEDULE_TRANSFER_TYPE,
    val version: Int = TRANSFER_VERSION,
    val records: List<CalendarItem> = emptyList(),
)

@Serializable
data class ColorPageTransfer(
    val type: String = COLOR_TRANSFER_TYPE,
    val version: Int = TRANSFER_VERSION,
    val colors: List<SavedColor> = emptyList(),
)

object SchedulePageTransferCodec {

    fun encode(records: List<CalendarItem>): String = json.encodeToString(
        SchedulePageTransfer.serializer(),
        SchedulePageTransfer(records = records.distinct()),
    )

    fun decode(text: String): List<CalendarItem> {
        val transfer = runCatching { json.decodeFromString(SchedulePageTransfer.serializer(), text) }
            .getOrNull() ?: throw IllegalArgumentException("Invalid schedule transfer")
        require(transfer.type == SCHEDULE_TRANSFER_TYPE) { "Invalid schedule transfer type" }
        require(transfer.version == TRANSFER_VERSION) { "Invalid schedule transfer version" }

        return transfer.records
            .also(::validateRecords)
            .distinct()
    }

    private fun validateRecords(records: List<CalendarItem>) {
        records.forEach { record ->
            require(record.weekday in 0..6) { "Invalid schedule weekday" }
            require(record.startMin in 0..END_MINUTE_OF_DAY) { "Invalid schedule start time" }
            require(record.endMin in 0..END_MINUTE_OF_DAY) { "Invalid schedule end time" }
            require(record.endMin > record.startMin) { "Invalid schedule time range" }
            require(record.title.isNotBlank()) { "Invalid schedule title" }
            require(record.title.length <= MAX_TEXT_LENGTH) { "Schedule title is too long" }
            require(record.teacher.isNullOrBlank() || record.teacher.length <= MAX_TEXT_LENGTH) {
                "Schedule teacher is too long"
            }
            require(record.classroom.isNullOrBlank() || record.classroom.length <= MAX_TEXT_LENGTH) {
                "Schedule classroom is too long"
            }
            require(record.note.isNullOrBlank() || record.note.length <= MAX_TEXT_LENGTH) {
                "Schedule note is too long"
            }
        }
    }
}

object ColorPageTransferCodec {

    fun encode(colors: List<SavedColor>): String = json.encodeToString(
        ColorPageTransfer.serializer(),
        ColorPageTransfer(colors = colors),
    )

    fun decode(text: String): List<SavedColor> {
        val transfer = runCatching { json.decodeFromString(ColorPageTransfer.serializer(), text) }
            .getOrNull() ?: throw IllegalArgumentException("Invalid color transfer")
        require(transfer.type == COLOR_TRANSFER_TYPE) { "Invalid color transfer type" }
        require(transfer.version == TRANSFER_VERSION) { "Invalid color transfer version" }
        validateColors(transfer.colors)
        return transfer.colors
    }

    private fun validateColors(colors: List<SavedColor>) {
        val sourceTypes = setOf("HEX", "RGB", "CMYK", "HSL", "HSV_HSB", "NCS", "LAB")
        require(colors.distinctBy { it.id }.size == colors.size) { "Duplicate color id" }
        colors.forEach { color ->
            require(color.id.isNotBlank()) { "Invalid color id" }
            require(color.name.isNotBlank() && color.name.length <= MAX_TEXT_LENGTH) {
                "Invalid color name"
            }
            require(ColorConverter.hexToRgb(color.hex) != null) { "Invalid color hex" }
            require(color.sourceType in sourceTypes) { "Invalid color source type" }
            require(color.createdAt >= 0L) { "Invalid color created time" }
        }
    }
}

@Serializable
data class LienzoColorEntry(
    val id: String,
    val name: String,
    val hex: String,
    val createdAt: Long,
)

object LienzoColorPaletteCodec {

    private val entryListSerializer = ListSerializer(LienzoColorEntry.serializer())

    fun encode(colors: List<SavedColor>): String = json.encodeToString(
        entryListSerializer,
        colors.map { color ->
            LienzoColorEntry(
                id = color.id,
                name = color.name,
                hex = color.hex,
                createdAt = color.createdAt,
            )
        },
    )

    fun decode(text: String): List<SavedColor> {
        val entries = runCatching { json.decodeFromString(entryListSerializer, text) }
            .getOrNull() ?: throw IllegalArgumentException("Invalid lienzo palette")
        require(entries.isNotEmpty()) { "Empty lienzo palette" }
        validateLienzoColors(entries)
        return entries.map { entry ->
            SavedColor(
                id = entry.id,
                name = entry.name,
                hex = entry.hex,
                sourceType = "HEX",
                sourceValue = null,
                createdAt = entry.createdAt,
            )
        }
    }

    private fun validateLienzoColors(colors: List<LienzoColorEntry>) {
        colors.forEach { color ->
            require(color.id.isNotBlank()) { "Invalid color id" }
            require(color.name.isNotBlank()) { "Invalid color name" }
            require(ColorConverter.hexToRgb(color.hex) != null) { "Invalid color hex" }
            require(color.createdAt >= 0L) { "Invalid color created time" }
        }
    }
}

private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
    prettyPrint = true
}

private const val TRANSFER_VERSION = 1
private const val SCHEDULE_TRANSFER_TYPE = "maplenilset.schedule"
private const val COLOR_TRANSFER_TYPE = "maplenilset.lienzo-color"
private const val END_MINUTE_OF_DAY = 24 * 60 - 1
private const val MAX_TEXT_LENGTH = 255
