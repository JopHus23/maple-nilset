package com.mapherdf.maplenilset.core.theme

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModelsTest {

    @Test
    fun `built in presets have separate light and dark source colors`() {
        val presets = listOf(
            ThemePreset.MAPLE,
            ThemePreset.CHERRY,
            ThemePreset.JADE,
            ThemePreset.TING_BLUE,
            ThemePreset.DYNAMIC,
        )

        assertEquals(presets + ThemePreset.CUSTOM, ThemePreset.entries)
        assertEquals(ThemePreset.MAPLE.id, UserThemeSettings().paletteId)
        assertEquals(
            listOf(ThemeColorFields.PRIMARY, ThemeColorFields.SECONDARY, ThemeColorFields.SURFACE),
            ThemeColorFields.EDITABLE,
        )
        presets.forEach { preset ->
            assertEquals(preset.lightColors, preset.colors(false))
            assertEquals(preset.darkColors, preset.colors(true))
            listOf(preset.lightColors, preset.darkColors).forEach { colors ->
                assertEquals(false, colors!!.background == colors.surface)
            }
        }
        assertEquals(true, UserThemeSettings().showCardBorders)
        assertEquals(true, UserThemeSettings().showConsoleBackground)
        assertEquals(12f, UserThemeSettings().consoleOutputFontSize)
        assertEquals(0.23f, UserThemeSettings().cardMaskOpacity)
    }

    @Test
    fun `mode controls light and dark source colors`() {
        val light = UserThemeSettings(mode = ThemeMode.LIGHT)
        val dark = UserThemeSettings(mode = ThemeMode.DARK)

        assertEquals(false, light.usesDarkTheme())
        assertEquals(true, dark.usesDarkTheme())
        assertEquals(true, UserThemeSettings(paletteId = ThemePreset.DYNAMIC.id).isDynamic)
    }

    @Test
    fun `color parser accepts only rgb hex`() {
        assertEquals("#D87C5F", ThemeColorParser.normalize("#d87c5f"))
        assertEquals(null, ThemeColorParser.normalize("D87C5F"))
        assertEquals(null, ThemeColorParser.normalize("#D87C5F0"))
        assertEquals(0xFFD87C5F.toInt(), ThemeColorParser.parseArgb("#D87C5F"))
    }

    @Test
    fun `custom colors are separated by light and dark mode`() {
        val lightColors = ThemeColors("#123456", "#654321", "#EEEEEE", "#FFFFFF")
        val darkColors = ThemeColors("#ABCDEF", "#123456", "#111111", "#222222")
        val settings = UserThemeSettings(
            paletteId = ThemePreset.CUSTOM.id,
            customLightColors = lightColors,
            customDarkColors = darkColors,
        )

        assertEquals(lightColors, settings.effectiveColors(useDark = false))
        assertEquals(darkColors, settings.effectiveColors(useDark = true))
    }

    @Test
    fun `custom theme defaults to maple source colors`() {
        val settings = UserThemeSettings(paletteId = ThemePreset.CUSTOM.id)

        assertEquals(ThemePreset.MAPLE.lightColors, settings.effectiveColors(useDark = false))
        assertEquals(ThemePreset.MAPLE.darkColors, settings.effectiveColors(useDark = true))
    }

    @Test
    fun `scale settings apply only when enabled`() {
        val disabled = UserThemeSettings(
            textScaleEnabled = false,
            textScale = 1.2f,
            uiScaleEnabled = false,
            uiScale = 0.8f,
        )
        val enabled = disabled.copy(
            textScaleEnabled = true,
            uiScaleEnabled = true,
        )

        assertEquals(1f, disabled.effectiveTextScale)
        assertEquals(1f, disabled.effectiveUiScale)
        assertEquals(1.2f, enabled.effectiveTextScale)
        assertEquals(0.8f, enabled.effectiveUiScale)
    }

    @Test
    fun `preset resolves source colors by mode`() {
        val settings = UserThemeSettings(paletteId = ThemePreset.JADE.id)

        assertEquals(ThemePreset.JADE.lightColors, settings.effectiveColors(useDark = false))
        assertEquals(ThemePreset.JADE.darkColors, settings.effectiveColors(useDark = true))
    }

    @Test
    fun `card mask opacity is clamped`() {
        val settings = UserThemeSettings(cardMaskOpacity = 1.4f)

        assertEquals(1f, settings.effectiveCardMaskOpacity)
        assertEquals(0f, settings.copy(cardMaskOpacity = -0.2f).effectiveCardMaskOpacity)
    }

    @Test
    fun `console output font size is clamped`() {
        val settings = UserThemeSettings(consoleOutputFontSize = 30f)

        assertEquals(24f, settings.effectiveConsoleOutputFontSize)
        assertEquals(
            10f,
            settings.copy(consoleOutputFontSize = 5f).effectiveConsoleOutputFontSize,
        )
    }

    @Test
    fun `theme settings serialize and deserialize`() {
        val settings = UserThemeSettings(
            mode = ThemeMode.DARK,
            paletteId = ThemePreset.JADE.id,
            customFontPath = "file:///data/user/0/com.mapherdf.maplenilset/files/font/custom.ttf",
            customFontName = "Custom Font.ttf",
            showConsoleBackground = false,
            consoleOutputFontSize = 18f,
            showCardBorders = false,
            cardMaskOpacity = 0.65f,
            customLightColors = ThemeColors("#123456", "#654321", "#EEEEEE", "#FFFFFF"),
            customDarkColors = ThemeColors("#ABCDEF", "#123456", "#111111", "#222222"),
        )
        val serialized = Json.encodeToString(UserThemeSettings.serializer(), settings)

        assertEquals(settings, Json.decodeFromString(UserThemeSettings.serializer(), serialized))
    }
}
