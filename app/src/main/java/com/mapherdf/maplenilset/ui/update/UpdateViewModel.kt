package com.mapherdf.maplenilset.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapherdf.maplenilset.BuildConfig
import com.mapherdf.maplenilset.data.update.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class UpToDate(val currentVersion: String) : UpdateState
    data class Available(
        val latestVersion: String,
        val downloadUrl: String,
    ) : UpdateState
    data class Downloading(val progress: Int) : UpdateState
    data class Downloaded(val file: File) : UpdateState
    data object Error : UpdateState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String = BuildConfig.VERSION_NAME

    fun checkForUpdate() {
        if (_state.value is UpdateState.Checking) return
        _state.value = UpdateState.Checking
        viewModelScope.launch {
            try {
                val updateInfo = updateRepository.checkForUpdate()
                _state.value = if (updateInfo != null) {
                    UpdateState.Available(
                        latestVersion = updateInfo.latestVersion,
                        downloadUrl = updateInfo.downloadUrl,
                    )
                } else {
                    UpdateState.UpToDate(BuildConfig.VERSION_NAME)
                }
            } catch (_: Exception) {
                _state.value = UpdateState.Error
            }
        }
    }

    fun startDownload(downloadUrl: String) {
        _state.value = UpdateState.Downloading(0)
        viewModelScope.launch {
            try {
                val file = updateRepository.downloadApk(downloadUrl) { progress ->
                    _state.value = UpdateState.Downloading(progress)
                }
                _state.value = UpdateState.Downloaded(file)
            } catch (_: Exception) {
                _state.value = UpdateState.Error
            }
        }
    }
}
