package com.mapherdf.maplenilset.bili.api

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class EncryptedBiliCookiePersistence(context: Context) : BiliCookiePersistence {

    private val json = Json { ignoreUnknownKeys = true }
    private val prefs: SharedPreferences
    private val legacyPrefs: SharedPreferences

    init {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            appContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        legacyPrefs = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        migrateLegacyCookies()
    }

    override fun load(): List<BiliStoredCookie> {
        return runCatching {
            val raw = prefs.getString(COOKIES_KEY, null) ?: return emptyList()
            json.decodeFromString<List<BiliStoredCookie>>(raw)
        }.getOrDefault(emptyList())
    }

    override fun save(cookies: List<BiliStoredCookie>) {
        runCatching {
            prefs.edit().putString(COOKIES_KEY, json.encodeToString(cookies)).apply()
        }
    }

    override fun clear() {
        runCatching { prefs.edit().remove(COOKIES_KEY).apply() }
        runCatching { legacyPrefs.edit().clear().apply() }
    }

    private fun migrateLegacyCookies() {
        if (prefs.contains(COOKIES_KEY) || legacyPrefs.all.isNullOrEmpty()) {
            return
        }
        runCatching {
            val migrated = legacyPrefs.all.mapNotNull { (_, rawValue) ->
                (rawValue as? String)?.let(::decodeLegacyCookie)
            }
            if (migrated.isNotEmpty()) {
                prefs.edit().putString(COOKIES_KEY, json.encodeToString(migrated)).apply()
            }
            legacyPrefs.edit().clear().apply()
        }
    }

    private fun decodeLegacyCookie(raw: String): BiliStoredCookie? {
        return runCatching {
            val domain = decode(raw).substringBefore(DELIMITER)
            val pair = decode(raw)
            BiliStoredCookie(
                name = pair.substringBefore("="),
                value = pair.substringAfter("="),
                domain = domain,
            )
        }.getOrNull()
    }

    private fun decode(raw: String): String =
        String(java.util.Base64.getUrlDecoder().decode(raw), Charsets.UTF_8)

    companion object {
        private const val PREFS_NAME = "bili_nil_secure_cookies"
        private const val LEGACY_PREFS_NAME = "bili_nil_cookies"
        private const val COOKIES_KEY = "cookies"
        private const val DELIMITER = "\u0000"
    }
}
