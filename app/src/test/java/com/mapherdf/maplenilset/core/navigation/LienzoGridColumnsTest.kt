package com.mapherdf.maplenilset.core.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class LienzoGridColumnsTest {

    @Test
    fun `pinch out decreases columns`() {
        assertEquals(3, targetLienzoColorGridColumns(currentColumns = 4, zoom = 1.25f))
    }

    @Test
    fun `pinch in increases columns`() {
        assertEquals(5, targetLienzoColorGridColumns(currentColumns = 4, zoom = 0.8f))
    }

    @Test
    fun `columns stay within supported range`() {
        assertEquals(
            MIN_LIENZO_COLOR_GRID_COLUMNS,
            targetLienzoColorGridColumns(currentColumns = 2, zoom = 100f),
        )
        assertEquals(
            MAX_LIENZO_COLOR_GRID_COLUMNS,
            targetLienzoColorGridColumns(currentColumns = 6, zoom = 0.1f),
        )
    }

    @Test
    fun `saved grid density round trips and normalizes out of range values`() {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        assertEquals(4, json.decodeFromString(UserUiSettings.serializer(), """{"lienzoColorGridColumns":4}""").lienzoColorGridColumns)
        assertEquals(DEFAULT_LIENZO_COLOR_GRID_COLUMNS, json.decodeFromString(UserUiSettings.serializer(), "{}").lienzoColorGridColumns)
    }
}
