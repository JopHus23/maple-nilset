package com.mapherdf.maplenilset.core.theme

import kotlinx.serialization.Serializable

object ThemeColorFields {
    const val PRIMARY = "primary"
    const val SECONDARY = "secondary"
    const val BACKGROUND = "background"
    const val SURFACE = "surface"

    val ALL = listOf(PRIMARY, SECONDARY, BACKGROUND, SURFACE)
    val EDITABLE = listOf(PRIMARY, SECONDARY, SURFACE)
}

@Serializable
data class ThemeColors(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String,
) {
    fun value(field: String): String? = when (field) {
        ThemeColorFields.PRIMARY -> primary
        ThemeColorFields.SECONDARY -> secondary
        ThemeColorFields.BACKGROUND -> background
        ThemeColorFields.SURFACE -> surface
        else -> null
    }
}

enum class ThemeMode {
    LIGHT,
    DARK,
}

enum class ThemePreset(
    val id: String,
    val label: String,
    val lightColors: ThemeColors?,
    val darkColors: ThemeColors?,
) {
    MAPLE(
        "maple",
        "枫糖",
        ThemeColors(
            primary = "#D87C5F",
            secondary = "#7D5C4F",
            background = "#FFF8F5",
            surface = "#FFEFE9",
        ),
        ThemeColors(
            primary = "#D87C5F",
            secondary = "#7D5C4F",
            background = "#191210",
            surface = "#231A16",
        ),
    ),
    CHERRY(
        "cherry",
        "落樱",
        ThemeColors(
            primary = "#F5C2D7",
            secondary = "#7A5A68",
            background = "#FFF8FA",
            surface = "#FFEDF4",
        ),
        ThemeColors(
            primary = "#F5C2D7",
            secondary = "#B39AA5",
            background = "#201619",
            surface = "#2A1E22",
        ),
    ),
    JADE(
        "jade",
        "青碧",
        ThemeColors(
            primary = "#009999",
            secondary = "#F5C2D7",
            background = "#F4FBFA",
            surface = "#E4F2F0",
        ),
        ThemeColors(
            primary = "#009999",
            secondary = "#D8BAC6",
            background = "#101C1A",
            surface = "#182624",
        ),
    ),
    TING_BLUE(
        "ting-blue",
        "汀蓝",
        ThemeColors(
            primary = "#1F6FEB",
            secondary = "#64748B",
            background = "#F5F8FC",
            surface = "#E9F0F8",
        ),
        ThemeColors(
            primary = "#4C8DF6",
            secondary = "#8FA5BF",
            background = "#0F151C",
            surface = "#18212C",
        ),
    ),
    DYNAMIC(
        "dynamic",
        "动态取色",
        ThemeColors(
            primary = "#6750A4",
            secondary = "#625B71",
            background = "#FFFBFE",
            surface = "#F3EDF7",
        ),
        ThemeColors(
            primary = "#D0BCFF",
            secondary = "#CCC2DC",
            background = "#141218",
            surface = "#211F26",
        ),
    ),
    CUSTOM("custom", "自定义", null, null);

    fun colors(useDark: Boolean): ThemeColors? {
        return if (useDark) darkColors else lightColors
    }

    companion object {
        fun fromId(id: String): ThemePreset =
            entries.firstOrNull { it.id == id } ?: MAPLE
    }
}

@Serializable
data class UserThemeSettings(
    val mode: ThemeMode = ThemeMode.DARK,
    val paletteId: String = ThemePreset.MAPLE.id,
    val customLightColors: ThemeColors? = null,
    val customDarkColors: ThemeColors? = null,
    val backgroundImageUri: String? = null,
    val backgroundOpacity: Float = DEFAULT_BACKGROUND_OPACITY,
    val customFontPath: String? = null,
    val customFontName: String? = null,
    val showConsoleBackground: Boolean = true,
    val showCardBorders: Boolean = true,
    val cardMaskOpacity: Float = DEFAULT_CARD_MASK_OPACITY,
    val textScaleEnabled: Boolean = false,
    val textScale: Float = 1f,
    val uiScaleEnabled: Boolean = false,
    val uiScale: Float = 1f,
    val consoleOutputFontSize: Float = DEFAULT_CONSOLE_OUTPUT_FONT_SIZE,
) {
    val palette: ThemePreset
        get() = ThemePreset.fromId(paletteId)

    val isDynamic: Boolean
        get() = palette == ThemePreset.DYNAMIC

    val effectiveTextScale: Float
        get() = if (textScaleEnabled) textScale.coerceIn(MIN_SCALE, MAX_SCALE) else 1f

    val effectiveUiScale: Float
        get() = if (uiScaleEnabled) uiScale.coerceIn(MIN_SCALE, MAX_SCALE) else 1f

    val effectiveConsoleOutputFontSize: Float
        get() = consoleOutputFontSize.coerceIn(
            MIN_CONSOLE_OUTPUT_FONT_SIZE,
            MAX_CONSOLE_OUTPUT_FONT_SIZE,
        )

    val effectiveCardMaskOpacity: Float
        get() = cardMaskOpacity.coerceIn(MIN_CARD_MASK_OPACITY, MAX_CARD_MASK_OPACITY)

    fun usesDarkTheme(): Boolean {
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    }

    fun effectiveColors(useDark: Boolean = usesDarkTheme()): ThemeColors {
        val colors = if (palette == ThemePreset.CUSTOM) {
            if (useDark) {
                customDarkColors ?: FALLBACK_DARK_COLORS
            } else {
                customLightColors ?: FALLBACK_LIGHT_COLORS
            }
        } else {
            palette.colors(useDark) ?: if (useDark) FALLBACK_DARK_COLORS else FALLBACK_LIGHT_COLORS
        }

        return colors
    }

    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 1.2f
        const val DEFAULT_SCALE = 1f
        const val MIN_BACKGROUND_OPACITY = 0f
        const val MAX_BACKGROUND_OPACITY = 1f
        const val DEFAULT_BACKGROUND_OPACITY = 1f
        const val MIN_CONSOLE_OUTPUT_FONT_SIZE = 10f
        const val MAX_CONSOLE_OUTPUT_FONT_SIZE = 24f
        const val DEFAULT_CONSOLE_OUTPUT_FONT_SIZE = 12f
        const val MIN_CARD_MASK_OPACITY = 0f
        const val MAX_CARD_MASK_OPACITY = 1f
        const val DEFAULT_CARD_MASK_OPACITY = 0.23f
        val FALLBACK_LIGHT_COLORS = ThemePreset.MAPLE.lightColors!!
        val FALLBACK_DARK_COLORS = ThemePreset.MAPLE.darkColors!!
    }
}

object ThemeColorParser {
    private val pattern = Regex("^#([0-9A-Fa-f]{6})$")

    fun normalize(value: String?): String? {
        val trimmed = value?.trim().orEmpty()
        val match = pattern.matchEntire(trimmed) ?: return null
        return "#${match.groupValues[1].uppercase()}"
    }

    fun parseArgb(value: String?): Int? {
        val normalized = normalize(value) ?: return null
        return normalized.substring(1).toLong(16).toInt() or 0xFF000000.toInt()
    }
}
