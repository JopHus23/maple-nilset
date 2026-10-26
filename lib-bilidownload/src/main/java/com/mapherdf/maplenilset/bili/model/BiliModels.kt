package com.mapherdf.maplenilset.bili.model

import kotlinx.serialization.Serializable

@Serializable
data class BiliVideoReference(
    val bvid: String,
    val aid: Long? = null,
    val page: Int = 1,
)

@Serializable
data class BiliVideoPage(
    val cid: Long,
    val page: Int,
    val part: String? = null,
    val duration: Long? = null,
)

@Serializable
data class BiliVideoOwner(
    val mid: Long? = null,
    val name: String? = null,
)

@Serializable
data class BiliVideoInfo(
    val bvid: String? = null,
    val aid: Long? = null,
    val title: String? = null,
    val pic: String? = null,
    val owner: BiliVideoOwner? = null,
    val pages: List<BiliVideoPage> = emptyList(),
)

@Serializable
data class BiliDashStream(
    val id: Int,
    @kotlinx.serialization.SerialName("baseUrl") val baseUrlValue: String? = null,
    @kotlinx.serialization.SerialName("base_url") val baseUrlSnake: String? = null,
    val bandwidth: Long? = null,
    val codecs: String? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    val resolvedUrl: String
        get() = baseUrlValue ?: baseUrlSnake.orEmpty()
}

@Serializable
data class BiliDashAudio(
    val id: Int,
    @kotlinx.serialization.SerialName("baseUrl") val baseUrlValue: String? = null,
    @kotlinx.serialization.SerialName("base_url") val baseUrlSnake: String? = null,
    val bandwidth: Long? = null,
    val codecs: String? = null,
) {
    val resolvedUrl: String
        get() = baseUrlValue ?: baseUrlSnake.orEmpty()
}

@Serializable
data class BiliDash(
    val video: List<BiliDashStream> = emptyList(),
    val audio: List<BiliDashAudio> = emptyList(),
)

@Serializable
data class BiliPlayUrlData(
    val accept_quality: List<Int> = emptyList(),
    val dash: BiliDash? = null,
    val durl: List<BiliDurlItem>? = null,
)

@Serializable
data class BiliDurlItem(
    val url: String? = null,
    val size: Long? = null,
    val length: Long? = null,
)

enum class BiliQuality(val code: Int, val label: String) {
    Q_8K(127, "8K"),
    Q_4K(120, "4K"),
    Q_1080P60(116, "1080P60"),
    Q_1080P_PLUS(112, "1080P+"),
    Q_1080P(80, "1080P"),
    Q_720P(64, "720P"),
    Q_480P(32, "480P"),
    Q_360P(16, "360P"),
    UNKNOWN(0, "unknown"),
    ;

    companion object {
        fun fromCode(code: Int): BiliQuality = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

enum class BiliAudioQuality(val code: Int, val label: String) {
    A_192K(30280, "192K"),
    A_132K(30232, "132K"),
    A_64K(30216, "64K"),
    UNKNOWN(0, "unknown"),
    ;

    companion object {
        fun fromCode(code: Int): BiliAudioQuality = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}