package com.mapherdf.maplenilset.bili.download

import kotlinx.serialization.Serializable
import com.mapherdf.maplenilset.bili.model.BiliMergeOutcome
import com.mapherdf.maplenilset.bili.model.BiliQuality
import com.mapherdf.maplenilset.bili.model.BiliVideoReference

enum class BiliTaskState {
    QUEUED,
    RESOLVING,
    DOWNLOADING,
    MERGING,
    EXPORTING,
    COMPLETED,
    FAILED,
    PAUSED,
    CANCELLED,
}

@Serializable
data class BiliDownloadRequest(
    val reference: BiliVideoReference,
    val qualityPriority: List<Int> = listOf(80, 64, 32, 16),
    val audioPriority: List<Int> = listOf(30280, 30232, 30216),
    val preferAvc: Boolean = true,
    val filenameTemplate: String = "{bvid}_p{page}_{quality}",
    val preResolvedCid: Long = 0,
    val preResolvedTitle: String = "",
    val preResolvedQualityLabel: String = "",
    val preResolvedVideoUrl: String? = null,
    val preResolvedAudioUrl: String? = null,
    val preResolvedVideoLength: Long = -1,
    val preResolvedAudioLength: Long = -1,
)

@Serializable
data class BiliTaskProgress(
    val videoBytesDownloaded: Long = 0,
    val videoBytesTotal: Long = -1,
    val audioBytesDownloaded: Long = 0,
    val audioBytesTotal: Long = -1,
    val speedBps: Long = 0,
    val etaSeconds: Long = -1,
)

@Serializable
data class BiliTaskSnapshot(
    val id: String,
    val request: BiliDownloadRequest,
    val title: String = "",
    val cid: Long = 0,
    val state: String = BiliTaskState.QUEUED.name,
    val progress: BiliTaskProgress = BiliTaskProgress(),
    val selectedQualityCode: Int = 0,
    val selectedAudioQualityCode: Int = 0,
    val mergeOutcome: String = BiliMergeOutcome.MERGED.name,
    val downgradeReason: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class BiliDownloadTask(
    val id: String,
    val request: BiliDownloadRequest,
    val title: String,
    val state: BiliTaskState,
    val progress: BiliTaskProgress,
    val quality: BiliQuality,
    val mergeOutcome: BiliMergeOutcome,
    val downgradeReason: String?,
    val outputUri: String?,
    val errorMessage: String?,
    val retryable: Boolean,
    val createdAt: Long,
)