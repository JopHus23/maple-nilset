package com.mapherdf.maplenilset.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberCustomBackgroundImage(
    uri: String?,
    retryKey: Any? = null,
): ImageBitmap? {
    val context = LocalContext.current
    val imageState: State<ImageBitmap?> = produceState<ImageBitmap?>(
        initialValue = null,
        key1 = uri,
        key2 = retryKey,
    ) {
        value = uri?.let { decodeImageBitmap(context, it) }
    }
    return imageState.value
}

private suspend fun decodeImageBitmap(context: Context, uri: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        runCatching {
            val parsedUri = Uri.parse(uri)
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(parsedUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@runCatching null
            }

            val displayMetrics = context.resources.displayMetrics
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(
                    width = bounds.outWidth,
                    height = bounds.outHeight,
                    requestedWidth = displayMetrics.widthPixels,
                    requestedHeight = displayMetrics.heightPixels,
                )
            }
            context.contentResolver.openInputStream(parsedUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

private fun calculateInSampleSize(
    width: Int,
    height: Int,
    requestedWidth: Int,
    requestedHeight: Int,
): Int {
    var inSampleSize = 1
    val longestSourceSide = maxOf(width, height)
    val longestRequestedSide = maxOf(requestedWidth, requestedHeight)
    var sampleLongestSide = longestSourceSide
    while (sampleLongestSide / 2 >= longestRequestedSide) {
        sampleLongestSide /= 2
        inSampleSize *= 2
    }
    return inSampleSize
}
