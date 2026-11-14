package com.mapherdf.maplenilset.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mapherdf.maplenilset.const.LocalStoreKeys
import com.mapherdf.maplenilset.core.notification.NotificationSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface NotificationSettingsRepository {
    val settings: Flow<NotificationSettings>

    suspend fun setScheduleNotificationsEnabled(enabled: Boolean)

    suspend fun setPersistentCourseEnabled(enabled: Boolean)

    suspend fun setReminderEnabled(enabled: Boolean)

    suspend fun setReminderLeadMinutes(minutes: Int)

    suspend fun setShowClassroom(enabled: Boolean)

    suspend fun setShowTeacher(enabled: Boolean)

    suspend fun setShowNote(enabled: Boolean)

    suspend fun reset()
}

@Singleton
class LocalNotificationSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : NotificationSettingsRepository {

    private val key = stringPreferencesKey(LocalStoreKeys.NOTIFICATION_SETTINGS)

    override val settings: Flow<NotificationSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            decode(preferences[key]) ?: NotificationSettings()
        }

    override suspend fun setScheduleNotificationsEnabled(enabled: Boolean) {
        update { it.copy(scheduleNotificationsEnabled = enabled) }
    }

    override suspend fun setPersistentCourseEnabled(enabled: Boolean) {
        update { it.copy(persistentCourseEnabled = enabled) }
    }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        update { it.copy(reminderEnabled = enabled) }
    }

    override suspend fun setReminderLeadMinutes(minutes: Int) {
        update { current ->
            current.copy(
                reminderLeadMinutes = minutes.coerceIn(
                    NotificationSettings.MIN_REMINDER_LEAD_MINUTES,
                    NotificationSettings.MAX_REMINDER_LEAD_MINUTES,
                ),
            )
        }
    }

    override suspend fun setShowClassroom(enabled: Boolean) {
        update { it.copy(showClassroom = enabled) }
    }

    override suspend fun setShowTeacher(enabled: Boolean) {
        update { it.copy(showTeacher = enabled) }
    }

    override suspend fun setShowNote(enabled: Boolean) {
        update { it.copy(showNote = enabled) }
    }

    override suspend fun reset() {
        update { NotificationSettings() }
    }

    private suspend fun update(transform: (NotificationSettings) -> NotificationSettings) {
        dataStore.edit { preferences ->
            val current = decode(preferences[key]) ?: NotificationSettings()
            preferences[key] = json.encodeToString(
                NotificationSettings.serializer(),
                transform(current),
            )
        }
    }

    private fun decode(serialized: String?): NotificationSettings? {
        if (serialized.isNullOrBlank()) return null
        return try {
            json.decodeFromString(NotificationSettings.serializer(), serialized)
        } catch (exception: SerializationException) {
            null
        } catch (exception: IllegalArgumentException) {
            null
        }
    }
}
