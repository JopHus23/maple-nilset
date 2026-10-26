package com.mapherdf.maplenilset.ui.theme

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import java.io.File

fun loadCustomFontFamily(path: String?): FontFamily {
    if (path.isNullOrBlank()) return AppFontFamily

    val parsedPath = android.net.Uri.parse(path)
    if (parsedPath.scheme != "file") return AppFontFamily

    return runCatching {
        val filePath = parsedPath.path ?: return AppFontFamily
        FontFamily(Typeface.createFromFile(File(filePath)))
    }.getOrDefault(AppFontFamily)
}
