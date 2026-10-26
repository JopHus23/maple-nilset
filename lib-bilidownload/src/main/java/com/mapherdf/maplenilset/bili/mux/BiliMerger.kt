package com.mapherdf.maplenilset.bili.mux

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.mapherdf.maplenilset.bili.api.BiliLogger
import com.mapherdf.maplenilset.bili.model.BiliMergeOutcome
import java.io.File
import java.nio.ByteBuffer

interface BiliMerger {
    suspend fun merge(
        videoFile: File,
        audioFile: File?,
        outputFile: File,
        rotationDegrees: Int = 0,
    ): BiliMergeResult
}

data class BiliMergeResult(
    val outcome: BiliMergeOutcome,
    val outputFile: File?,
    val reason: String? = null,
)

class MediaMuxerMerger : BiliMerger {

    override suspend fun merge(
        videoFile: File,
        audioFile: File?,
        outputFile: File,
        rotationDegrees: Int,
    ): BiliMergeResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (!videoFile.exists()) {
            return@withContext BiliMergeResult(BiliMergeOutcome.FAILED, null, "Video file not found")
        }

        val videoExtractor = MediaExtractor()
        val audioExtractor = audioFile?.let { if (it.exists()) MediaExtractor() else null }

        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            val videoTrack = findTrackIndex(videoExtractor, "video/")
            if (videoTrack < 0) {
                return@withContext BiliMergeResult(BiliMergeOutcome.FAILED, null, "No video track found")
            }
            val videoFormat = videoExtractor.getTrackFormat(videoTrack)
            val videoMime = videoFormat.getString(MediaFormat.KEY_MIME) ?: ""

            if (!isMuxerSupported(videoMime)) {
                return@withContext BiliMergeResult(
                    BiliMergeOutcome.SEPARATE,
                    null,
                    "Video codec $videoMime is not supported by MediaMuxer",
                )
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            if (audioExtractor != null && audioFile != null) {
                audioExtractor.setDataSource(audioFile.absolutePath)
                audioTrackIndex = findTrackIndex(audioExtractor, "audio/")
                if (audioTrackIndex >= 0) {
                    audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)
                    val audioMime = audioFormat?.getString(MediaFormat.KEY_MIME) ?: ""
                    if (!isMuxerSupported(audioMime)) {
                        audioTrackIndex = -1
                        audioFormat = null
                    }
                }
            }

            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) outputFile.delete()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            if (rotationDegrees != 0) muxer.setOrientationHint(rotationDegrees)

            val videoMuxerTrack = muxer.addTrack(videoFormat)
            val audioMuxerTrack = if (audioTrackIndex >= 0 && audioFormat != null) muxer.addTrack(audioFormat) else -1

            muxer.start()

            writeTrack(videoExtractor, videoTrack, muxer, videoMuxerTrack)
            if (audioTrackIndex >= 0 && audioMuxerTrack >= 0) {
                writeTrack(audioExtractor!!, audioTrackIndex, muxer, audioMuxerTrack)
            }

            muxer.stop()
            muxer.release()

            BiliMergeResult(BiliMergeOutcome.MERGED, outputFile)
        } catch (e: Exception) {
            BiliLogger.e(TAG, "Merge failed", e)
            outputFile.delete()
            BiliMergeResult(BiliMergeOutcome.FAILED, null, e.message ?: "Merge failed")
        } finally {
            videoExtractor.release()
            audioExtractor?.release()
        }
    }

    private fun findTrackIndex(extractor: MediaExtractor, prefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith(prefix)) return i
        }
        return -1
    }

    private fun isMuxerSupported(mime: String): Boolean =
        mime.startsWith("video/avc") || mime.startsWith("video/hevc") ||
            mime.startsWith("audio/mp4a") || mime.startsWith("audio/mpeg") ||
            mime.startsWith("audio/raw")

    private fun writeTrack(
        extractor: MediaExtractor,
        trackIndex: Int,
        muxer: MediaMuxer,
        muxerTrackIndex: Int,
    ) {
        extractor.selectTrack(trackIndex)
        val maxBufferSize = extractor.getTrackFormat(trackIndex).let {
            if (it.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) it.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else DEFAULT_BUFFER_SIZE
        }
        val buffer = ByteBuffer.allocate(maxBufferSize.coerceAtLeast(DEFAULT_BUFFER_SIZE))
        val info = android.media.MediaCodec.BufferInfo()
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            info.offset = 0
            info.size = sampleSize
            info.presentationTimeUs = extractor.sampleTime
            info.flags = when (extractor.sampleFlags) {
                android.media.MediaExtractor.SAMPLE_FLAG_SYNC -> android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME
                else -> 0
            }
            muxer.writeSampleData(muxerTrackIndex, buffer, info)
            if (!extractor.advance()) break
        }
        extractor.unselectTrack(trackIndex)
    }

    companion object {
        private const val TAG = "MediaMuxerMerger"
        private const val DEFAULT_BUFFER_SIZE = 65536
    }
}