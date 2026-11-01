package com.mapherdf.maplenilset.data.color

import kotlinx.serialization.Serializable

@Serializable
data class SavedColor(
    val id: String,
    val name: String,
    val hex: String,
    val sourceType: String,
    val sourceValue: String? = null,
    val createdAt: Long,
)
