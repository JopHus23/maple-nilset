package com.mapherdf.maplenilset.core.bili

enum class BiliContentKind {
    AV,
    BV,
    LIVE,
}

data class BiliInput(
    val kind: BiliContentKind,
    val id: String,
) {
    val displayName: String
        get() = when (kind) {
            BiliContentKind.AV -> "av$id"
            BiliContentKind.BV -> "BV$id"
            BiliContentKind.LIVE -> "live$id"
        }
}

object BiliInputParser {

    private val avBare = Regex("""^(?:av)?([0-9]+)$""", RegexOption.IGNORE_CASE)
    private val bvBare = Regex("""^(?:bv)([0-9A-Za-z]{10})$""", RegexOption.IGNORE_CASE)
    private val avUrl = Regex(
        """(?:https?://)?(?:www\.|m\.)?bilibili\.com/(?:video/)?av([0-9]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val bvUrl = Regex(
        """(?:https?://)?(?:www\.|m\.)?bilibili\.com/(?:video/)?BV([0-9A-Za-z]{10})""",
        RegexOption.IGNORE_CASE,
    )
    private val liveUrl = Regex(
        """(?:https?://)?live\.bilibili\.com/(?:h5/)?([0-9]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val b23Url = Regex("""(?:https?://)?(?:www\.)?b23\.tv/([0-9A-Za-z]+)""", RegexOption.IGNORE_CASE)

    fun parse(rawInput: String): BiliInput? = parseShortLinkCode(rawInput)?.let { null }
        ?: parseContent(rawInput)

    fun parseShortLinkCode(rawInput: String): String? {
        return b23Url.find(rawInput.trim())?.groupValues?.get(1)
    }

    fun parseContent(rawInput: String): BiliInput? {
        val input = rawInput.trim()
        return bvBare.matchEntire(input)?.let { BiliInput(BiliContentKind.BV, it.groupValues[1]) }
            ?: bvUrl.find(input)?.let { BiliInput(BiliContentKind.BV, it.groupValues[1]) }
            ?: avBare.matchEntire(input)?.let { BiliInput(BiliContentKind.AV, it.groupValues[1]) }
            ?: avUrl.find(input)?.let { BiliInput(BiliContentKind.AV, it.groupValues[1]) }
            ?: liveUrl.find(input)?.let { BiliInput(BiliContentKind.LIVE, it.groupValues[1]) }
    }
}
