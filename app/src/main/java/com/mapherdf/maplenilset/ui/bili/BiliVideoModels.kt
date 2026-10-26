package com.mapherdf.maplenilset.ui.bili

import androidx.compose.ui.graphics.ImageBitmap
import com.mapherdf.maplenilset.bili.download.BiliTaskProgress
import com.mapherdf.maplenilset.bili.download.BiliTaskState
import com.mapherdf.maplenilset.bili.model.BiliMergeOutcome
import com.mapherdf.maplenilset.bili.model.BiliQuality
import com.mapherdf.maplenilset.bili.model.BiliVideoInfo

data class BiliVideoUiState(
    val inputText: String = "",
    val isResolving: Boolean = false,
    val videoInfo: BiliVideoInfo? = null,
    val coverPreview: ImageBitmap? = null,
    val isCoverLoading: Boolean = false,
    val availableQualities: List<BiliQuality> = emptyList(),
    val downloadableQualityCodes: Set<Int> = emptySet(),
    val resolvedCid: Long = 0,
    val resolvedTitle: String = "",
    val resolvedQualityLabel: String = "",
    val cachedPlayUrl: com.mapherdf.maplenilset.bili.model.BiliPlayUrlData? = null,
    val resolvedVideoUrl: String? = null,
    val resolvedAudioUrl: String? = null,
    val resolvedVideoLength: Long = -1,
    val resolvedAudioLength: Long = -1,
    val selectedQuality: BiliQuality = BiliQuality.Q_1080P,
    val isEnqueuing: Boolean = false,
    val showLoginPrompt: Boolean = false,
    val errorMessage: String? = null,
    val activeTaskId: String? = null,
    val taskState: BiliTaskState? = null,
    val taskProgress: BiliTaskProgress? = null,
    val mergeOutcome: BiliMergeOutcome? = null,
    val downgradeReason: String? = null,
    val completedOutputPath: String? = null,
)
