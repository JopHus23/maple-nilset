package com.mapherdf.maplenilset.ui.bili

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.data.repository.BiliNilRepository
import com.mapherdf.maplenilset.data.remote.bili.BiliCoverException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class BiliNilViewModel @Inject constructor(
    private val repository: BiliNilRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiliNilUiState())
    val uiState: StateFlow<BiliNilUiState> = _uiState.asStateFlow()

    private var resolveJob: Job? = null
    private var downloadJob: Job? = null

    fun updateInput(value: String) {
        _uiState.update { it.copy(inputText = value) }
    }

    fun resolve() {
        val state = _uiState.value
        if (state.isResolving || state.inputText.isBlank()) return

        resolveJob?.cancel()
        _uiState.update {
            it.copy(
                isResolving = true,
                errorMessageRes = null,
                savedFileName = null,
            )
        }
        resolveJob = viewModelScope.launch {
            try {
                val details = repository.resolve(state.inputText)
                val coverFile = repository.downloadCover(details)
                val preview = BiliCoverImageLoader.decode(coverFile.file)
                    ?: throw BiliCoverException(BILI_ERROR_CODE, "Unsupported cover image")
                _uiState.update {
                    it.copy(
                        isResolving = false,
                        details = details,
                        coverFile = coverFile,
                        preview = preview,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isResolving = false,
                        errorMessageRes = errorMessageRes(exception),
                    )
                }
            }
        }
    }

    fun download() {
        val state = _uiState.value
        if (state.isDownloading || state.details == null) return

        downloadJob?.cancel()
        _uiState.update {
            it.copy(isDownloading = true, errorMessageRes = null, savedFileName = null)
        }
        downloadJob = viewModelScope.launch {
            try {
                val details = state.details
                val coverFile = state.coverFile ?: repository.downloadCover(details)
                val savedName = repository.saveCover(details, coverFile).getOrThrow()
                _uiState.update { it.copy(isDownloading = false, savedFileName = savedName) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(isDownloading = false, errorMessageRes = errorMessageRes(exception))
                }
            }
        }
    }

    @StringRes
    private fun errorMessageRes(exception: Exception): Int = when (exception) {
        is BiliCoverException -> when {
            exception.code == 0 -> R.string.bili_error_invalid_input
            exception.code == 412 -> R.string.bili_error_risk_control
            exception.code == -404 -> R.string.bili_error_not_found
            else -> R.string.bili_error_upstream
        }
        is IOException -> R.string.bili_error_network
        is HttpException -> R.string.bili_error_network
        else -> R.string.bili_error_unknown
    }

    private companion object {
        const val BILI_ERROR_CODE = 1
    }
}
