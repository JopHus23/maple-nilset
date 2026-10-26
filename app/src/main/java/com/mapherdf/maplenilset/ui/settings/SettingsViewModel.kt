package com.mapherdf.maplenilset.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapherdf.maplenilset.core.theme.ThemeColors
import com.mapherdf.maplenilset.core.theme.ThemeColorFields
import com.mapherdf.maplenilset.core.theme.ThemeColorParser
import com.mapherdf.maplenilset.core.theme.ThemeMode
import com.mapherdf.maplenilset.core.theme.UserThemeSettings
import com.mapherdf.maplenilset.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
) : ViewModel() {

    val themeSettings: StateFlow<UserThemeSettings> = themeRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserThemeSettings(),
        )

    fun setThemeMode(mode: ThemeMode) = launch { themeRepository.setThemeMode(mode) }

    fun setCardBorders(enabled: Boolean) = launch { themeRepository.setCardBorders(enabled) }

    fun setCardMaskOpacity(value: Float) = launch { themeRepository.setCardMaskOpacity(value) }

    fun selectPalette(paletteId: String) = launch { themeRepository.selectPalette(paletteId) }

    fun saveCustomColors(values: Map<String, String>, useDarkTheme: Boolean) {
        val current = themeSettings.value
        val fallback = current.effectiveColors(useDarkTheme)
        val colors = ThemeColors(
            primary = ThemeColorParser.normalize(values[ThemeColorFields.PRIMARY])
                ?: fallback.primary,
            secondary = ThemeColorParser.normalize(values[ThemeColorFields.SECONDARY])
                ?: fallback.secondary,
            background = ThemeColorParser.normalize(values[ThemeColorFields.BACKGROUND])
                ?: fallback.background,
            surface = ThemeColorParser.normalize(values[ThemeColorFields.SURFACE])
                ?: fallback.surface,
        )
        launch { themeRepository.setCustomColors(colors, useDarkTheme) }
    }

    fun resetCustomColors(useDarkTheme: Boolean) =
        launch { themeRepository.resetCustomColors(useDarkTheme) }

    fun setTextScaleEnabled(enabled: Boolean) =
        launch { themeRepository.setTextScaleEnabled(enabled) }

    fun setTextScale(scale: Float) = launch { themeRepository.setTextScale(scale) }

    fun setUiScaleEnabled(enabled: Boolean) =
        launch { themeRepository.setUiScaleEnabled(enabled) }

    fun setUiScale(scale: Float) = launch { themeRepository.setUiScale(scale) }

    suspend fun applyCroppedBackgroundImage(
        sourceUri: String,
        cropLeft: Float,
        cropTop: Float,
        cropRight: Float,
        cropBottom: Float,
    ): String = themeRepository.applyCroppedBackgroundImage(
        sourceUri = sourceUri,
        cropLeft = cropLeft,
        cropTop = cropTop,
        cropRight = cropRight,
        cropBottom = cropBottom,
    )

    fun removeCustomBackgroundImage() = launch { themeRepository.resetCustomBackgroundImage() }

    fun setBackgroundOpacity(value: Float) = launch { themeRepository.setBackgroundOpacity(value) }

    fun applyCustomFont(sourceUri: String) = launch { themeRepository.applyCustomFont(sourceUri) }

    fun removeCustomFont() = launch { themeRepository.resetCustomFont() }

    fun setConsoleBackground(enabled: Boolean) =
        launch { themeRepository.setConsoleBackground(enabled) }

    fun setConsoleOutputFontSize(fontSize: Float) =
        launch { themeRepository.setConsoleOutputFontSize(fontSize) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
