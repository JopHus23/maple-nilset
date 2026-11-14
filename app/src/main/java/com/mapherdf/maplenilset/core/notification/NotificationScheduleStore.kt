package com.mapherdf.maplenilset.core.notification

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mapherdf.maplenilset.const.LocalStoreKeys
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Singleton
class NotificationScheduleStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val key = stringPreferencesKey(LocalStoreKeys.NOTIFICATION_TRIGGER_IDS)
    private val firedReminderKey =
        stringPreferencesKey(LocalStoreKeys.NOTIFICATION_FIRED_TRIGGER_IDS)
    private val serializer = ListSerializer(Int.serializer())
    private val firedReminderMutex = Mutex()

    suspend fun readIds(): List<Int> = dataStore.data.map { preferences ->
        decode(preferences[key])
    }.first()

    suspend fun saveIds(ids: List<Int>) {
        dataStore.edit { preferences ->
            preferences[key] = json.encodeToString(serializer, ids.distinct())
        }
    }

    suspend fun markReminderFiredOnce(triggerId: Int): Boolean =
        firedReminderMutex.withLock {
            val firedIds = dataStore.data.map { preferences ->
                decode(preferences[firedReminderKey])
            }.first()
            if (triggerId in firedIds) return@withLock false

            dataStore.edit { preferences ->
                preferences[firedReminderKey] = json.encodeToString(
                    serializer,
                    (firedIds + triggerId).takeLast(199),
                )
            }
            true
        }

    private fun decode(serialized: String?): List<Int> {
        if (serialized.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(serializer, serialized)
        }.getOrDefault(emptyList())
    }
}
