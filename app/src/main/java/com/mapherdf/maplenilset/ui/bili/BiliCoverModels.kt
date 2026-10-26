package com.mapherdf.maplenilset.ui.bili

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.ImageBitmap
import com.mapherdf.maplenilset.data.remote.bili.BiliCoverDetails
import com.mapherdf.maplenilset.data.remote.bili.BiliCoverFile

data class BiliNilUiState(
    val inputText: String = "",
    val isResolving: Boolean = false,
    val isDownloading: Boolean = false,
    val details: BiliCoverDetails? = null,
    val coverFile: BiliCoverFile? = null,
    val preview: ImageBitmap? = null,
    @StringRes val errorMessageRes: Int? = null,
    val savedFileName: String? = null,
)
