package com.mapherdf.maplenilset.data.downloads

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.mapherdf.maplenilset.const.BiliExpressions
import com.mapherdf.maplenilset.core.bili.BiliCoverMappers
import com.mapherdf.maplenilset.core.bili.BiliInput
import com.mapherdf.maplenilset.data.remote.bili.BiliCoverFile
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiliCoverMediaSaver @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun save(
        source: BiliCoverFile,
        input: BiliInput,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val displayName = availableDisplayName(
                BiliCoverMappers.coverFileName(input.kind, input.id, source.extension),
            )
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, source.mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, BiliExpressions.MEDIA_RELATIVE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Could not create download entry")
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    source.file.inputStream().use { sourceStream ->
                        sourceStream.copyTo(output)
                    }
                } ?: throw IOException("Could not open download output")

                val completeValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(uri, completeValues, null, null)
                Result.success(displayName)
            } catch (exception: CancellationException) {
                runCatching { resolver.delete(uri, null, null) }
                throw exception
            } catch (exception: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                Result.failure(exception)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private fun availableDisplayName(preferredName: String): String {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val baseName = preferredName.substringBeforeLast('.')
        val extension = preferredName.substringAfterLast('.', "jpg")

        fun displayName(index: Int): String =
            if (index == 0) preferredName else "$baseName ($index).$extension"

        for (index in 0..999) {
            val candidate = displayName(index)
            val cursor = resolver.query(
                collection,
                projection,
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND ${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf("${BiliExpressions.MEDIA_RELATIVE_PATH}/", candidate),
                null,
            ) ?: return candidate
            cursor.use {
                if (it.count == 0) return candidate
            }
        }
        throw IOException("Too many downloads with the same name")
    }
}
