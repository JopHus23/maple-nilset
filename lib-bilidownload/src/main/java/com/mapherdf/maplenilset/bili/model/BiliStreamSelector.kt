package com.mapherdf.maplenilset.bili.model

import com.mapherdf.maplenilset.bili.api.BiliApiException

enum class BiliCodecPreference {
    AVC,
    HEVC,
    AV1,
    ANY,
    ;

    companion object {
        fun fromCodecString(codec: String?): BiliCodecPreference = when {
            codec == null -> ANY
            codec.startsWith("avc", ignoreCase = true) -> AVC
            codec.startsWith("hev", ignoreCase = true) || codec.startsWith("hvc", ignoreCase = true) -> HEVC
            codec.startsWith("av01", ignoreCase = true) || codec.startsWith("av", ignoreCase = true) -> AV1
            else -> ANY
        }
    }
}

enum class BiliMergeOutcome {
    MERGED,
    SEPARATE,
    FAILED,
}

data class BiliStreamSelection(
    val videoStream: BiliDashStream,
    val audioStream: BiliDashAudio?,
    val selectedQuality: BiliQuality,
    val selectedAudioQuality: BiliAudioQuality?,
    val mergeOutcome: BiliMergeOutcome = BiliMergeOutcome.MERGED,
    val downgradeReason: String? = null,
    val videoContentLength: Long = -1,
    val audioContentLength: Long = -1,
)

class BiliStreamSelector {

    fun select(
        dash: com.mapherdf.maplenilset.bili.model.BiliDash,
        qualityPriority: List<BiliQuality>,
        audioPriority: List<BiliAudioQuality>,
        preferAvc: Boolean = true,
    ): BiliStreamSelection {
        val availableVideos = dash.video.filter { it.resolvedUrl.isNotBlank() }
        val availableAudios = dash.audio
            .filter { it.resolvedUrl.isNotBlank() }
            .filter { it.codecs?.contains("flac", ignoreCase = true) != true }
            .filter { it.codecs?.contains("dolby", ignoreCase = true) != true }

        for (targetQuality in qualityPriority) {
            val candidates = availableVideos.filter { it.id == targetQuality.code }
            if (candidates.isEmpty()) continue

            val preferredCodec = if (preferAvc) {
                candidates.firstOrNull { BiliCodecPreference.fromCodecString(it.codecs) == BiliCodecPreference.AVC }
                    ?: candidates.firstOrNull { BiliCodecPreference.fromCodecString(it.codecs) == BiliCodecPreference.HEVC }
                    ?: candidates.firstOrNull { BiliCodecPreference.fromCodecString(it.codecs) == BiliCodecPreference.AV1 }
                    ?: candidates.first()
            } else {
                candidates.firstOrNull { BiliCodecPreference.fromCodecString(it.codecs) != BiliCodecPreference.AV1 }
                    ?: candidates.first()
            }

            if (preferAvc &&
                BiliCodecPreference.fromCodecString(preferredCodec.codecs) == BiliCodecPreference.AV1
            ) {
                continue
            }

            val audio = selectBestAudio(availableAudios, audioPriority)
            return BiliStreamSelection(
                videoStream = preferredCodec,
                audioStream = audio,
                selectedQuality = targetQuality,
                selectedAudioQuality = audio?.let { BiliAudioQuality.fromCode(it.id) },
                mergeOutcome = BiliMergeOutcome.MERGED,
            )
        }

        val fallbackVideo = availableVideos.firstOrNull()
            ?: throw BiliApiException(-1, "No downloadable video stream found")
        val fallbackAudio = selectBestAudio(availableAudios, audioPriority)
        val isAv1Fallback = BiliCodecPreference.fromCodecString(fallbackVideo.codecs) == BiliCodecPreference.AV1
        val fallbackQuality = BiliQuality.fromCode(fallbackVideo.id)

        return BiliStreamSelection(
            videoStream = fallbackVideo,
            audioStream = fallbackAudio,
            selectedQuality = fallbackQuality,
            selectedAudioQuality = fallbackAudio?.let { BiliAudioQuality.fromCode(it.id) },
            mergeOutcome = if (isAv1Fallback) BiliMergeOutcome.SEPARATE else BiliMergeOutcome.MERGED,
            downgradeReason = if (isAv1Fallback) "所有可用画质均仅有 AV1 编码，保持音视频分离" else null,
        )
    }

    fun autoDowngradeForAvc(
        dash: com.mapherdf.maplenilset.bili.model.BiliDash,
        requestedQuality: BiliQuality,
        audioPriority: List<BiliAudioQuality>,
    ): BiliStreamSelection? {
        val priority = BiliQuality.entries
            .filter { it != BiliQuality.UNKNOWN }
            .filter { it.code < requestedQuality.code }
            .sortedByDescending { it.code }
        val result = select(dash, priority, audioPriority, preferAvc = true)
        return if (result.mergeOutcome == BiliMergeOutcome.MERGED) {
            result.copy(downgradeReason = "请求画质仅有 AV1 编码，已自动降级至 ${result.selectedQuality.label}")
        } else null
    }

    private fun selectBestAudio(
        audios: List<com.mapherdf.maplenilset.bili.model.BiliDashAudio>,
        priority: List<BiliAudioQuality>,
    ): com.mapherdf.maplenilset.bili.model.BiliDashAudio? {
        for (target in priority) {
            audios.firstOrNull { it.id == target.code }?.let { return it }
        }
        return audios.maxByOrNull { it.bandwidth ?: 0 }
    }
}
