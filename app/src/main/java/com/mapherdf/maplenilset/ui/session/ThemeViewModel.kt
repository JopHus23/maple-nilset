package com.mapherdf.maplenilset.ui.session

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import com.mapherdf.maplenilset.core.theme.UserThemeSettings
import com.mapherdf.maplenilset.data.repository.ThemeRepository
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    themeRepository: ThemeRepository,
) : ViewModel() {
    val themeSettings: StateFlow<UserThemeSettings> = themeRepository.settings
    val isThemeReady: StateFlow<Boolean> = themeRepository.isReady
}
