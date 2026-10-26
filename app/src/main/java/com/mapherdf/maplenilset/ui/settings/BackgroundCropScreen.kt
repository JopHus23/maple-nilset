package com.mapherdf.maplenilset.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.ui.theme.rememberCustomBackgroundImage
import com.mapherdf.maplenilset.ui.theme.themeContainerColor
private data class CropSelection(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
) {
    companion object {
        const val MIN_SIZE_PX = 96
    }
}

private enum class CropDragMode {
    NONE,
    MOVE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundCropScreen(
    sourceUri: String,
    onApplied: () -> Unit,
    onDiscard: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var decodeAttempt by remember(sourceUri) { mutableStateOf(0) }
    val image = rememberCustomBackgroundImage(sourceUri, decodeAttempt)
    val coroutineScope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val configuration = LocalConfiguration.current
    val targetAspectRatio = configuration.screenWidthDp.toFloat() / configuration.screenHeightDp.toFloat()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var dragMode by remember { mutableStateOf(CropDragMode.NONE) }
    var selection by remember(sourceUri) { mutableStateOf(CropSelection(0.12f, 0.10f, 0.88f, 0.80f)) }
    var isApplying by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }
    var isDecodeFailed by remember(sourceUri, decodeAttempt) { mutableStateOf(false) }

    LaunchedEffect(image, sourceUri, decodeAttempt) {
        if (image == null) {
            delay(600)
            isDecodeFailed = true
        } else {
            isDecodeFailed = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.background_crop_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeContainerColor(),
                ),
                navigationIcon = {
                    IconButton(onClick = onDiscard) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.background_crop_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val currentImage = image
            if (currentImage == null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDecodeFailed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.background_crop_load_failed),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            OutlinedButton(onClick = { decodeAttempt++ }) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    } else {
                        CircularProgressIndicator()
                    }
                }
            } else {
                val aspectRatio = currentImage.width.toFloat() / currentImage.height.toFloat()
                LaunchedEffect(currentImage, targetAspectRatio) {
                    selection = initialCropSelection(
                        imageAspectRatio = aspectRatio,
                        targetAspectRatio = targetAspectRatio,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(sourceUri, canvasSize) {
                            detectDragGestures(
                                onDragStart = { position ->
                                    dragMode = cropDragModeFor(
                                        position = position,
                                        selection = selection,
                                        canvasSize = canvasSize,
                                    )
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    if (canvasSize == IntSize.Zero || dragMode == CropDragMode.NONE) {
                                        return@detectDragGestures
                                    }

                                    selection = updateCropSelection(
                                        selection = selection,
                                        mode = dragMode,
                                        dragAmount = amount,
                                        canvasSize = canvasSize,
                                        targetAspectRatio = targetAspectRatio,
                                    )
                                },
                                onDragEnd = {
                                    dragMode = CropDragMode.NONE
                                },
                                onDragCancel = {
                                    dragMode = CropDragMode.NONE
                                },
                            )
                        },
                ) {
                    Image(
                        bitmap = currentImage,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.matchParentSize(),
                    )

                    Canvas(modifier = Modifier.matchParentSize()) {
                        val left = selection.startX * size.width
                        val top = selection.startY * size.height
                        val right = selection.endX * size.width
                        val bottom = selection.endY * size.height
                        val dimColor = Color.Black.copy(alpha = 0.55f)

                        drawRect(dimColor, topLeft = Offset.Zero, size = Size(size.width, top))
                        drawRect(
                            dimColor,
                            topLeft = Offset(0f, bottom),
                            size = Size(size.width, size.height - bottom),
                        )
                        drawRect(
                            dimColor,
                            topLeft = Offset(0f, top),
                            size = Size(left, bottom - top),
                        )
                        drawRect(
                            dimColor,
                            topLeft = Offset(right, top),
                            size = Size(size.width - right, bottom - top),
                        )

                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top),
                            style = Stroke(width = 2.dp.toPx()),
                        )

                        listOf(
                            Offset(left, top),
                            Offset(right, top),
                            Offset(left, bottom),
                            Offset(right, bottom),
                        ).forEach { corner ->
                            drawCircle(
                                color = primaryColor,
                                radius = 7.dp.toPx(),
                                center = corner,
                            )
                        }
                    }
                }
            }

            if (isFailed) {
                Text(
                    text = stringResource(R.string.background_crop_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        currentImage?.let { image ->
                            selection = initialCropSelection(
                                imageAspectRatio = image.width.toFloat() / image.height.toFloat(),
                                targetAspectRatio = targetAspectRatio,
                            )
                            isFailed = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(R.string.background_crop_reset))
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isApplying = true
                            isFailed = false
                            val appliedUri = viewModel.applyCroppedBackgroundImage(
                                sourceUri = sourceUri,
                                cropLeft = selection.startX,
                                cropTop = selection.startY,
                                cropRight = selection.endX,
                                cropBottom = selection.endY,
                            )
                            isApplying = false
                            if (appliedUri != null) {
                                onApplied()
                            } else {
                                isFailed = true
                            }
                        }
                    },
                    enabled = currentImage != null && !isApplying,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.background_crop_apply))
                    }
                }
            }
        }
    }
}

private fun cropDragModeFor(
    position: Offset,
    selection: CropSelection,
    canvasSize: IntSize,
): CropDragMode {
    if (canvasSize == IntSize.Zero) return CropDragMode.NONE
    val left = selection.startX * canvasSize.width
    val top = selection.startY * canvasSize.height
    val right = selection.endX * canvasSize.width
    val bottom = selection.endY * canvasSize.height
    val handleRadius = (minOf(canvasSize.width, canvasSize.height) * 0.06f).coerceAtLeast(36f)

    return when {
        position.distanceSquared(Offset(left, top)) <= handleRadius * handleRadius -> CropDragMode.TOP_LEFT
        position.distanceSquared(Offset(right, top)) <= handleRadius * handleRadius -> CropDragMode.TOP_RIGHT
        position.distanceSquared(Offset(left, bottom)) <= handleRadius * handleRadius -> CropDragMode.BOTTOM_LEFT
        position.distanceSquared(Offset(right, bottom)) <= handleRadius * handleRadius -> CropDragMode.BOTTOM_RIGHT
        position.x in left..right && position.y in top..bottom -> CropDragMode.MOVE
        else -> CropDragMode.NONE
    }
}

private fun initialCropSelection(
    imageAspectRatio: Float,
    targetAspectRatio: Float,
): CropSelection {
    if (imageAspectRatio <= 0f || targetAspectRatio <= 0f) {
        return CropSelection(0f, 0f, 1f, 1f)
    }

    val spanX: Float
    val spanY: Float
    if (imageAspectRatio >= targetAspectRatio) {
        spanY = 1f
        spanX = targetAspectRatio / imageAspectRatio
    } else {
        spanX = 1f
        spanY = imageAspectRatio / targetAspectRatio
    }

    return CropSelection(
        startX = (1f - spanX) / 2f,
        startY = (1f - spanY) / 2f,
        endX = (1f + spanX) / 2f,
        endY = (1f + spanY) / 2f,
    )
}

private fun updateCropSelection(
    selection: CropSelection,
    mode: CropDragMode,
    dragAmount: Offset,
    canvasSize: IntSize,
    targetAspectRatio: Float,
): CropSelection {
    if (canvasSize == IntSize.Zero || targetAspectRatio <= 0f) return selection
    val deltaX = dragAmount.x / canvasSize.width
    val deltaY = dragAmount.y / canvasSize.height

    if (mode == CropDragMode.MOVE) {
        val boundedX = deltaX.coerceIn(-selection.startX, 1f - selection.endX)
        val boundedY = deltaY.coerceIn(-selection.startY, 1f - selection.endY)
        return selection.copy(
            startX = selection.startX + boundedX,
            startY = selection.startY + boundedY,
            endX = selection.endX + boundedX,
            endY = selection.endY + boundedY,
        )
    }
    if (mode == CropDragMode.NONE) return selection

    val movingXIsStart = mode == CropDragMode.TOP_LEFT || mode == CropDragMode.BOTTOM_LEFT
    val movingYIsStart = mode == CropDragMode.TOP_LEFT || mode == CropDragMode.TOP_RIGHT
    val anchorX = if (movingXIsStart) selection.endX else selection.startX
    val anchorY = if (movingYIsStart) selection.endY else selection.startY
    val targetEdgeX = (if (movingXIsStart) selection.startX else selection.endX) + deltaX
    val targetEdgeY = (if (movingYIsStart) selection.startY else selection.endY) + deltaY
    val desiredSpanX = abs(targetEdgeX - anchorX)
    val desiredSpanY = abs(targetEdgeY - anchorY)

    val maxSpanX = if (movingXIsStart) anchorX else 1f - anchorX
    val maxSpanY = if (movingYIsStart) anchorY else 1f - anchorY
    val minSpanX = CropSelection.MIN_SIZE_PX.toFloat() / canvasSize.width
    val minSpanY = CropSelection.MIN_SIZE_PX.toFloat() / canvasSize.height

    fun fitFromSpanX(spanX: Float): Pair<Float, Float> {
        var fittedX = spanX.coerceIn(minSpanX, maxSpanX)
        var fittedY = fittedX * canvasSize.width / (canvasSize.height * targetAspectRatio)
        if (fittedY > maxSpanY) {
            fittedY = maxSpanY
            fittedX = fittedY * canvasSize.height * targetAspectRatio / canvasSize.width
        }
        if (fittedY < minSpanY) {
            fittedY = minSpanY
            fittedX = fittedY * canvasSize.height * targetAspectRatio / canvasSize.width
        }
        return fittedX.coerceIn(minSpanX, maxSpanX) to fittedY.coerceIn(minSpanY, maxSpanY)
    }

    fun fitFromSpanY(spanY: Float): Pair<Float, Float> {
        var fittedY = spanY.coerceIn(minSpanY, maxSpanY)
        var fittedX = fittedY * canvasSize.height * targetAspectRatio / canvasSize.width
        if (fittedX > maxSpanX) {
            fittedX = maxSpanX
            fittedY = fittedX * canvasSize.width / (canvasSize.height * targetAspectRatio)
        }
        if (fittedX < minSpanX) {
            fittedX = minSpanX
            fittedY = fittedX * canvasSize.width / (canvasSize.height * targetAspectRatio)
        }
        return fittedX.coerceIn(minSpanX, maxSpanX) to fittedY.coerceIn(minSpanY, maxSpanY)
    }

    val fromX = fitFromSpanX(desiredSpanX)
    val fromY = fitFromSpanY(desiredSpanY)
    val (spanX, spanY) = if (fromX.first >= fromY.first) fromX else fromY

    val newStartX = if (movingXIsStart) anchorX - spanX else anchorX
    val newEndX = if (movingXIsStart) anchorX else anchorX + spanX
    val newStartY = if (movingYIsStart) anchorY - spanY else anchorY
    val newEndY = if (movingYIsStart) anchorY else anchorY + spanY

    return selection.copy(
        startX = newStartX,
        startY = newStartY,
        endX = newEndX,
        endY = newEndY,
    )
}

private fun Offset.distanceSquared(other: Offset): Float {
    val deltaX = x - other.x
    val deltaY = y - other.y
    return deltaX * deltaX + deltaY * deltaY
}
