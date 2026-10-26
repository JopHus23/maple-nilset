package com.mapherdf.maplenilset.ui.bili

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object BiliCoverImageLoader {

    suspend fun decode(file: File, maxDimension: Int = 1600): ImageBitmap? =
        withContext(Dispatchers.Default) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            }
            BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
        }

    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var longestSide = maxOf(width, height)
        while (longestSide / 2 >= maxDimension) {
            sampleSize *= 2
            longestSide /= 2
        }
        return sampleSize
    }
}
