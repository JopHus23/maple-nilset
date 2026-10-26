package com.mapherdf.maplenilset.bili.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BiliApiEnvelope(
    val code: Int = Int.MIN_VALUE,
    val message: String? = null,
    val data: JsonElement? = null,
)