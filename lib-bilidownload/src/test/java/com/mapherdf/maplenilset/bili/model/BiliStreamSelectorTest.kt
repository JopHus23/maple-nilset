package com.mapherdf.maplenilset.bili.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BiliStreamSelectorTest {

    @Test
    fun `prefers lower quality with avc when requested quality is av1 only`() {
        val dash = BiliDash(
            video = listOf(
                BiliDashStream(id = 120, baseUrlValue = "av1-4k", codecs = "av01"),
                BiliDashStream(id = 32, baseUrlValue = "avc-480p", codecs = "avc1"),
            ),
            audio = listOf(BiliDashAudio(id = 30280, baseUrlValue = "audio")),
        )

        val result = BiliStreamSelector().select(
            dash = dash,
            qualityPriority = listOf(BiliQuality.Q_4K, BiliQuality.Q_480P),
            audioPriority = listOf(BiliAudioQuality.A_192K),
            preferAvc = true,
        )

        assertEquals(BiliQuality.Q_480P, result.selectedQuality)
        assertEquals(BiliMergeOutcome.MERGED, result.mergeOutcome)
    }

    @Test
    fun `keeps separate when all available qualities are av1 only`() {
        val dash = BiliDash(
            video = listOf(
                BiliDashStream(id = 120, baseUrlValue = "av1-4k", codecs = "av01"),
                BiliDashStream(id = 32, baseUrlValue = "av1-480p", codecs = "av01"),
            ),
            audio = listOf(BiliDashAudio(id = 30280, baseUrlValue = "audio")),
        )

        val result = BiliStreamSelector().select(
            dash = dash,
            qualityPriority = listOf(BiliQuality.Q_4K, BiliQuality.Q_480P),
            audioPriority = listOf(BiliAudioQuality.A_192K),
            preferAvc = true,
        )

        assertEquals(BiliQuality.Q_4K, result.selectedQuality)
        assertEquals(BiliMergeOutcome.SEPARATE, result.mergeOutcome)
    }
}
