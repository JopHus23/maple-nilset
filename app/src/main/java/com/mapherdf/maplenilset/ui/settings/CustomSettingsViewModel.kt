package com.mapherdf.maplenilset.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapherdf.maplenilset.core.navigation.HomeDestination
import com.mapherdf.maplenilset.core.navigation.HomeDestinationMove
import com.mapherdf.maplenilset.core.navigation.UserUiSettings
import com.mapherdf.maplenilset.data.repository.UiSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CustomSettingsViewModel @Inject constructor(
    private val uiSettingsRepository: UiSettingsRepository,
) : ViewModel() {

    val uiSettings: StateFlow<UserUiSettings> = uiSettingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserUiSettings(),
        )

    fun selectStartupPage(page: HomeDestination) =
        launch { uiSettingsRepository.selectStartupPage(page) }

    fun movePage(page: HomeDestination, direction: HomeDestinationMove) =
        launch { uiSettingsRepository.movePage(page, direction) }

    fun setPageVisible(page: HomeDestination, visible: Boolean) =
        launch { uiSettingsRepository.setPageVisible(page, visible) }

    fun resetUiSettings() =
        launch { uiSettingsRepository.reset() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
