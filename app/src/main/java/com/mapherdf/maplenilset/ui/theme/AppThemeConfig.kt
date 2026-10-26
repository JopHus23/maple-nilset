package com.mapherdf.maplenilset.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily

/**
 * 可替换的主题配置：深色配色、字体、文字大小均由此定义。
 * 后续接入设置页后，可通过切换此实例来自定义或选择预设主题。
 * 当前仅有 [Default] 一个预设。
 */
data class AppThemeConfig(
    val colorScheme: ColorScheme,
    val typography: Typography,
) {
    companion object {
        val Default = AppThemeConfig(
            colorScheme = defaultDarkColorScheme(),
            typography = buildTypography(AppFontFamily),
        )

        // TODO: 后续扩展更多预设，如 OLED 纯黑 / 浅色 / 跟随系统等
    }
}
