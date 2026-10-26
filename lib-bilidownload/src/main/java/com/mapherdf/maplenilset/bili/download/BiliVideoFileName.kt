package com.mapherdf.maplenilset.bili.download

internal object BiliVideoFileName {

    fun create(title: String?, bvid: String, qualityLabel: String): String {
        val safeTitle = sanitize(title.orEmpty())
            .ifBlank { sanitize(bvid).ifBlank { "video" } }
        val safeBvid = sanitize(bvid).ifBlank { "video" }
        val safeQualityLabel = sanitize(qualityLabel).ifBlank { "unknown" }
        return "$safeTitle{$safeBvid}[$safeQualityLabel].mp4"
    }

    private fun sanitize(value: String): String =
        value.replace(INVALID_WINDOWS_CHARS, "_")
            .trim()
            .trimEnd('.', ' ')

    private val INVALID_WINDOWS_CHARS = Regex("[\\\\/:*?\"<>|\u0000-\u001F]")
}
