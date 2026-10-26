package com.mapherdf.maplenilset.bili.api

import kotlinx.serialization.Serializable

@Serializable
data class BiliStoredCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val expiresAt: Long = -1L,
    val secure: Boolean = true,
    val httpOnly: Boolean = false,
    val hostOnly: Boolean = false,
)

interface BiliCookiePersistence {
    fun load(): List<BiliStoredCookie>
    fun save(cookies: List<BiliStoredCookie>)
    fun clear()
}
