package com.mapherdf.maplenilset.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.mapherdf.maplenilset.const.MarkdownFiles
import com.mapherdf.maplenilset.data.repository.MarkdownRepository

sealed interface HomeTextUiState {
    data object Loading : HomeTextUiState

    data class Success(val text: String) : HomeTextUiState

    data object Error : HomeTextUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val markdownRepository: MarkdownRepository,
) : ViewModel() {
    private val _homeText = MutableStateFlow<HomeTextUiState>(HomeTextUiState.Loading)
    val homeText: StateFlow<HomeTextUiState> = _homeText.asStateFlow()

    init {
        loadHomeText()
    }

    fun reloadHomeText() {
        loadHomeText()
    }

    private fun loadHomeText() {
        viewModelScope.launch {
            _homeText.value = markdownRepository
                .read(MarkdownFiles.HOME_PLACEHOLDER)
                .fold(
                    onSuccess = { HomeTextUiState.Success(it) },
                    onFailure = { HomeTextUiState.Error },
                )
        }
    }
}
