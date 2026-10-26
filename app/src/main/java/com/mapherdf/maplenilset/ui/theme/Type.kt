package com.mapherdf.maplenilset.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mapherdf.maplenilset.R

val AppFontFamily = FontFamily(
    Font(R.font.jetbrains_maple_mono_medium)
)

private fun appStyle(
    fontFamily: FontFamily,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
) = TextStyle(
    fontFamily = fontFamily,
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
)

fun buildTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = appStyle(fontFamily, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = appStyle(fontFamily, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = appStyle(fontFamily, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = appStyle(fontFamily, FontWeight.Bold, 52.sp, 60.sp),
    headlineMedium = appStyle(fontFamily, FontWeight.Bold, 28.sp, 36.sp),
    headlineSmall = appStyle(fontFamily, FontWeight.Bold, 24.sp, 32.sp),
    titleLarge = appStyle(fontFamily, FontWeight.Bold, 20.sp, 28.sp),
    titleMedium = appStyle(fontFamily, FontWeight.Medium, 16.sp, 24.sp),
    titleSmall = appStyle(fontFamily, FontWeight.Medium, 14.sp, 20.sp),
    bodyLarge = appStyle(fontFamily, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = appStyle(fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = appStyle(fontFamily, fontSize = 13.sp, lineHeight = 20.sp),
    labelLarge = appStyle(fontFamily, FontWeight.Medium, 14.sp, 20.sp),
    labelMedium = appStyle(fontFamily, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = appStyle(fontFamily, FontWeight.Medium, 11.sp, 16.sp),
)
