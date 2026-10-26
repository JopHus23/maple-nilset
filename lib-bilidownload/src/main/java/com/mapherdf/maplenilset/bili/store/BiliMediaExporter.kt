package com.mapherdf.maplenilset.bili.store

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mapherdf.maplenilset.bili.api.BiliLogger
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class BiliMediaExporter(private val context: Context) {

    fun exportVideo(sourceFile: File, displayName: String): Uri? {
        val sanitized = sanitizeFilename(displayName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportViaMediaStore(sourceFile, sanitized)
        } else {
            exportToPublicDirectory(sourceFile, sanitized)
        }
    }

    private fun exportViaMediaStore(sourceFile: File, displayName: String): Uri? {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Nilset/Video")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
        try {
            resolver.openOutputStream(uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: return null
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            BiliLogger.e(TAG, "MediaStore export failed", e)
            resolver.delete(uri, null, null)
            return null
        }
    }

    private fun exportToPublicDirectory(sourceFile: File, displayName: String): Uri? {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val targetDir = File(moviesDir, "Nilset/Video")
        if (!targetDir.exists()) targetDir.mkdirs()
        val targetFile = File(targetDir, displayName)
        try {
            sourceFile.copyTo(targetFile, overwrite = true)
            return Uri.fromFile(targetFile)
        } catch (e: Exception) {
            BiliLogger.e(TAG, "Public directory export failed", e)
            return null
        }
    }

    companion object {
        private const val TAG = "BiliMediaExporter"
        private val ILLEGAL_FILENAME_CHARS = Regex("""[\\/:*?\"<>|]""")

        fun sanitizeFilename(raw: String): String {
            var result = ILLEGAL_FILENAME_CHARS.replace(raw, "_")
            result = result.trim('.', ' ')
            if (result.isBlank()) result = "nilset_video"
            if (!result.endsWith(".mp4")) result += ".mp4"
            return result
        }
    }
}