package com.mapherdf.maplenilset.core.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class UserUiSettingsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `missing lienzo color view defaults to list`() {
        val settings = json.decodeFromString(UserUiSettings.serializer(), "{}")

        assertEquals(LienzoColorView.LIST, settings.lienzoColorView)
    }

    @Test
    fun `saved grid view is decoded`() {
        val settings = json.decodeFromString(
            UserUiSettings.serializer(),
            """{"lienzoColorView":"GRID"}""",
        )

        assertEquals(LienzoColorView.GRID, settings.lienzoColorView)
    }
}
