package com.mapherdf.maplenilset.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mapherdf.maplenilset.const.LocalStoreKeys
import com.mapherdf.maplenilset.core.navigation.HomeDestination
import com.mapherdf.maplenilset.core.navigation.HomeDestinationMove
import com.mapherdf.maplenilset.core.navigation.UserUiSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

interface UiSettingsRepository {
    val settings: Flow<UserUiSettings>

    suspend fun selectStartupPage(page: HomeDestination)

    suspend fun movePage(page: HomeDestination, direction: HomeDestinationMove)

    suspend fun setPageVisible(page: HomeDestination, visible: Boolean)

    suspend fun reset()
}

@Singleton
class LocalUiSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) : UiSettingsRepository {

    private val key = stringPreferencesKey(LocalStoreKeys.UI_SETTINGS)

    override val settings: Flow<UserUiSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            val serialized = preferences[key]
            if (serialized.isNullOrBlank()) {
                UserUiSettings()
            } else {
                try {
                    json.decodeFromString(UserUiSettings.serializer(), serialized)
                } catch (exception: SerializationException) {
                    UserUiSettings()
                } catch (exception: IllegalArgumentException) {
                    UserUiSettings()
                }
            }.normalized()
        }

    override suspend fun selectStartupPage(page: HomeDestination) {
        update { current ->
            current.copy(startupPage = page)
        }
    }

    override suspend fun movePage(page: HomeDestination, direction: HomeDestinationMove) {
        update { current ->
            val currentIndex = current.pageOrder.indexOf(page)
            val targetIndex = when (direction) {
                HomeDestinationMove.UP -> currentIndex - 1
                HomeDestinationMove.DOWN -> currentIndex + 1
            }
            if (currentIndex < 0 || targetIndex !in current.pageOrder.indices) {
                return@update current
            }
            val newOrder = current.pageOrder.toMutableList()
            newOrder[currentIndex] = newOrder[targetIndex]
            newOrder[targetIndex] = page
            current.copy(pageOrder = newOrder)
        }
    }

    override suspend fun setPageVisible(page: HomeDestination, visible: Boolean) {
        update { current ->
            if (!visible && current.visiblePages.size <= 1) {
                return@update current
            }
            val hiddenPages = if (visible) {
                current.hiddenPages - page
            } else {
                current.hiddenPages + page
            }
            current.copy(hiddenPages = hiddenPages)
        }
    }

    override suspend fun reset() {
        update { UserUiSettings() }
    }

    private suspend fun update(transform: (UserUiSettings) -> UserUiSettings) {
        dataStore.edit { preferences ->
            val current = decode(preferences[key]) ?: UserUiSettings()
            preferences[key] = json.encodeToString(
                UserUiSettings.serializer(),
                transform(current).normalized(),
            )
        }
    }

    private fun decode(serialized: String?): UserUiSettings? {
        if (serialized.isNullOrBlank()) return null
        return try {
            json.decodeFromString(UserUiSettings.serializer(), serialized)
        } catch (exception: SerializationException) {
            null
        } catch (exception: IllegalArgumentException) {
            null
        }
    }

    private fun UserUiSettings.normalized(): UserUiSettings {
        val defaultOrder = HomeDestination.entries
        val order = pageOrder.filter { it in defaultOrder } + defaultOrder.filterNot { it in pageOrder }
        val hiddenPages = hiddenPages.filter { it in order }.toSet()
        val visiblePages = order.filterNot { it in hiddenPages }
        val adjustedHiddenPages = if (visiblePages.isEmpty()) {
            hiddenPages - (order.firstOrNull() ?: HomeDestination.HOME)
        } else {
            hiddenPages
        }
        val adjustedVisiblePages = order.filterNot { it in adjustedHiddenPages }
        val startupPage = startupPage
            .takeIf { it in adjustedVisiblePages }
            ?: adjustedVisiblePages.firstOrNull()
            ?: HomeDestination.HOME
        return UserUiSettings(
            pageOrder = order,
            hiddenPages = adjustedHiddenPages,
            startupPage = startupPage,
        )
    }
}
