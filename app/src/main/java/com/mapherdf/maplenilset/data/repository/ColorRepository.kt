package com.mapherdf.maplenilset.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mapherdf.maplenilset.data.color.SavedColor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

interface ColorRepository {
    val savedColors: Flow<List<SavedColor>>

    suspend fun addColor(color: SavedColor)

    suspend fun updateColor(color: SavedColor)

    suspend fun removeColor(colorId: String)
}

@Singleton
class LocalColorRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) : ColorRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("saved_colors")

    override val savedColors: Flow<List<SavedColor>> = dataStore.data.map { preferences ->
        preferences[key]?.let { serialized ->
            runCatching { json.decodeFromString(ListSerializer(SavedColor.serializer()), serialized) }
                .getOrDefault(emptyList())
        } ?: emptyList()
    }

    override suspend fun addColor(color: SavedColor) {
        dataStore.edit { preferences ->
            val current = preferences[key]?.let { serialized ->
                runCatching { json.decodeFromString(ListSerializer(SavedColor.serializer()), serialized) }
                    .getOrDefault(emptyList())
            } ?: emptyList()
            preferences[key] = json.encodeToString(ListSerializer(SavedColor.serializer()), current + color)
        }
    }

    override suspend fun updateColor(color: SavedColor) {
        dataStore.edit { preferences ->
            val current = preferences[key]?.let { serialized ->
                runCatching { json.decodeFromString(ListSerializer(SavedColor.serializer()), serialized) }
                    .getOrDefault(emptyList())
            } ?: emptyList()
            val updated = current.map { if (it.id == color.id) color else it }
            preferences[key] = json.encodeToString(ListSerializer(SavedColor.serializer()), updated)
        }
    }

    override suspend fun removeColor(colorId: String) {
        dataStore.edit { preferences ->
            val current = preferences[key]?.let { serialized ->
                runCatching { json.decodeFromString(ListSerializer(SavedColor.serializer()), serialized) }
                    .getOrDefault(emptyList())
            } ?: emptyList()
            val updated = current.filterNot { it.id == colorId }
            preferences[key] = json.encodeToString(ListSerializer(SavedColor.serializer()), updated)
        }
    }
}
