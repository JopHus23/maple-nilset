package com.mapherdf.maplenilset.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mapherdf.maplenilset.const.LocalStoreKeys
import com.mapherdf.maplenilset.data.calendar.CalendarItem
import com.mapherdf.maplenilset.data.calendar.LocalCalendar
import com.mapherdf.maplenilset.data.calendar.UserCalendar
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalCalendarRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : CalendarRepository {

    override suspend fun getCalendar(): Result<UserCalendar> = runCatching {
        val key = stringPreferencesKey(LocalStoreKeys.CALENDAR)
        val serialized = dataStore.data.first()[key]
        if (serialized.isNullOrBlank()) {
            UserCalendar(calendarId = 0L, records = emptyList(), isInitialized = false)
        } else {
            val calendar = json.decodeFromString(LocalCalendar.serializer(), serialized)
            UserCalendar(
                calendarId = 0L,
                records = calendar.records.map { record ->
                    if (record.weekday == 7) record.copy(weekday = 0) else record
                },
            )
        }
    }

    override suspend fun saveCalendar(records: List<CalendarItem>): Result<Unit> = runCatching {
        val key = stringPreferencesKey(LocalStoreKeys.CALENDAR)
        dataStore.edit { preferences ->
            preferences[key] = json.encodeToString(
                LocalCalendar.serializer(),
                LocalCalendar(records = records, isInitialized = true),
            )
        }
    }

    override suspend fun deleteCalendar(): Result<Unit> = runCatching {
        val key = stringPreferencesKey(LocalStoreKeys.CALENDAR)
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
