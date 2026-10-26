package com.mapherdf.maplenilset.core.bili

import com.mapherdf.maplenilset.const.BiliExpressions

object BiliCoverMappers {

    fun isAllowedImageUrl(rawUrl: String?): Boolean {
        val url = rawUrl?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        return url.startsWith("https://", ignoreCase = true) &&
            url.removePrefix("https://").substringBefore('/').let { host ->
                host.equals(BiliExpressions.IMAGE_HOST, ignoreCase = true) ||
                    host.endsWith(".${BiliExpressions.IMAGE_HOST}", ignoreCase = true)
            }
    }

    fun coverFileName(kind: BiliContentKind, id: String, extension: String): String {
        val safeId = id.replace(Regex("""[^0-9A-Za-z_-]"""), "_")
        val safeExtension = extension.removePrefix(".").ifBlank { "jpg" }
        return "nilset-bili-cover-${kind.name.lowercase()}-$safeId.$safeExtension"
    }
}
