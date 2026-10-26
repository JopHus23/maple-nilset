package com.mapherdf.maplenilset.data.remote.bili

import kotlinx.serialization.Serializable
import com.mapherdf.maplenilset.core.bili.BiliContentKind
import java.io.File

@Serializable
data class BiliVideoOwner(
    val mid: Long? = null,
    val name: String? = null,
)

@Serializable
data class BiliVideoData(
    val bvid: String? = null,
    val aid: Long? = null,
    val pic: String? = null,
    val title: String? = null,
    val desc: String? = null,
    val owner: BiliVideoOwner? = null,
)

@Serializable
data class BiliVideoEnvelope(
    override val code: Int = Int.MIN_VALUE,
    override val message: String? = null,
    val data: BiliVideoData? = null,
) : BiliEnvelope

@Serializable
data class BiliLiveData(
    val user_cover: String? = null,
    val title: String? = null,
    val uid: Long? = null,
)

@Serializable
data class BiliLiveEnvelope(
    override val code: Int = Int.MIN_VALUE,
    override val message: String? = null,
    val data: BiliLiveData? = null,
) : BiliEnvelope

interface BiliEnvelope {
    val code: Int
    val message: String?
}

data class BiliCoverDetails(
    val input: BiliInputReference,
    val title: String,
    val imageUrl: String,
    val author: String? = null,
    val uid: Long? = null,
    val description: String? = null,
)

data class BiliInputReference(
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

data class BiliCoverFile(
    val file: File,
    val mimeType: String,
    val extension: String,
)

class BiliCoverException(
    val code: Int,
    message: String,
) : IllegalStateException(message)