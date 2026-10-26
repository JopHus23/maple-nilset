package com.mapherdf.maplenilset.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.mapherdf.maplenilset.core.theme.ThemeColorFields
import com.mapherdf.maplenilset.core.theme.ThemeColorParser
import com.mapherdf.maplenilset.core.theme.ThemeColors
import com.mapherdf.maplenilset.core.theme.UserThemeSettings

fun defaultDarkColorScheme() = darkColorScheme(
    primary = AccentWarm,
    onPrimary = OnAccentWarm,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = OutlineDark,
)

val LocalCardMaskOpacity = staticCompositionLocalOf {
    UserThemeSettings.DEFAULT_CARD_MASK_OPACITY
}

@Composable
fun ATOMTheme(
    config: AppThemeConfig = AppThemeConfig.Default,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDarkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        else -> config.colorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = config.typography,
        content = content,
    )
}

@Composable
fun ATOMTheme(
    settings: UserThemeSettings,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useDarkTheme = settings.usesDarkTheme()
    val colorScheme = when {
        settings.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            remember(context, useDarkTheme) {
                if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
        else -> remember(settings, useDarkTheme) { settings.toColorScheme(useDarkTheme) }
    }

    val currentDensity = LocalDensity.current
    val scaledDensity = Density(
        density = currentDensity.density * settings.effectiveUiScale,
        fontScale = currentDensity.fontScale * settings.effectiveTextScale,
    )
    val customFontFamily = remember(settings.customFontPath) {
        loadCustomFontFamily(settings.customFontPath)
    }
    val typography = remember(customFontFamily) {
        buildTypography(customFontFamily)
    }

    CompositionLocalProvider(
        LocalDensity provides scaledDensity,
        LocalCardMaskOpacity provides settings.effectiveCardMaskOpacity,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

private fun UserThemeSettings.toColorScheme(useDark: Boolean): ColorScheme {
    val colors = effectiveColors(useDark)
    val background = colors.backgroundColor()
    val surface = colors.surfaceColor()
    val primary = colors.primaryColor()
    val secondary = colors.secondaryColor()
    val onBackground = background.contrastText()
    val onSurface = surface.contrastText()
    val isDark = useDark
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    val primaryContainer = primary.blend(surface, 0.72f)
    val secondaryContainer = secondary.blend(surface, 0.72f)
    val black = Color.Black
    val white = Color.White

    return base.copy(
        primary = primary,
        onPrimary = primary.contrastText(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.contrastText(),
        secondary = secondary,
        onSecondary = secondary.contrastText(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.contrastText(),
        tertiary = secondary,
        onTertiary = secondary.contrastText(),
        tertiaryContainer = secondaryContainer,
        onTertiaryContainer = secondaryContainer.contrastText(),
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surface.blend(onSurface, 0.16f),
        onSurfaceVariant = onSurface.blend(surface, 0.28f),
        surfaceDim = surface.blend(black, 0.08f),
        surfaceBright = surface.blend(if (isDark) onSurface else white, 0.08f),
        surfaceContainerLowest = surface.blend(if (isDark) black else white, 0.08f),
        surfaceContainerLow = surface.blend(onSurface, 0.14f),
        surfaceContainer = surface.blend(onSurface, if (isDark) 0.20f else 0.18f),
        surfaceContainerHigh = surface.blend(onSurface, if (isDark) 0.26f else 0.24f),
        surfaceContainerHighest = surface.blend(onSurface, 0.32f),
        outline = surface.blend(onSurface, 0.48f),
        outlineVariant = surface.blend(onSurface, 0.24f),
        scrim = Color.Black,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        inversePrimary = primary.blend(background, 0.8f),
    )
}

@Composable
fun themeContainerColor(): Color {
    val background = MaterialTheme.colorScheme.background
    val adjustmentTarget = if (background.luminance() < 0.5f) Color.White else Color.Black
    return background.blend(adjustmentTarget, 0.24f).copy(alpha = LocalCardMaskOpacity.current)
}

@Composable
fun themeDrawerMaskColor(): Color {
    return MaterialTheme.colorScheme.background.copy(alpha = 0.82f)
}

@Composable
fun themeContainerBorderColor(): Color {
    val background = MaterialTheme.colorScheme.background
    val adjustmentTarget = if (background.luminance() < 0.5f) Color.White else Color.Black
    return background.blend(adjustmentTarget, 0.48f).copy(alpha = 0.40f)
}

private fun ThemeColors.color(field: String): Color {
    val normalized = ThemeColorParser.normalize(value(field))
    val argb = ThemeColorParser.parseArgb(normalized)
    return argb?.let(::Color) ?: Color(0xFF16161E)
}

private fun ThemeColors.primaryColor() = color(ThemeColorFields.PRIMARY)

private fun ThemeColors.secondaryColor() = color(ThemeColorFields.SECONDARY)

private fun ThemeColors.backgroundColor() = color(ThemeColorFields.BACKGROUND)

private fun ThemeColors.surfaceColor() = color(ThemeColorFields.SURFACE)

private fun Color.blend(other: Color, fraction: Float): Color {
    val source = copy(alpha = 1f)
    val target = other.copy(alpha = 1f)
    return Color(
        red = source.red + (target.red - source.red) * fraction,
        green = source.green + (target.green - source.green) * fraction,
        blue = source.blue + (target.blue - source.blue) * fraction,
        alpha = 1f,
    )
}

private fun Color.contrastText(): Color {
    return if (luminance() > 0.5f) Color(0xFF241915) else Color.White
}
