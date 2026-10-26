package com.mapherdf.maplenilset.ui.schedule

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

private val NumberWheelItemHeight = 40.dp
private const val NumberWheelVisibleItemCount = 3

@Composable
fun ScheduleNumberWheel(
    value: Int,
    maxValue: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val safeValue = value.coerceIn(0, maxValue)
    val scrollPosition = remember(maxValue) { Animatable(safeValue.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentValue by rememberUpdatedState(value)
    val currentMaxValue by rememberUpdatedState(maxValue)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val itemHeightPx = with(LocalDensity.current) {
        NumberWheelItemHeight.toPx().roundToInt()
    }
    val wheelShape = RoundedCornerShape(10.dp)
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (enabled) 1f else 0.38f,
    )
    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            val target = (scrollPosition.value - delta / itemHeightPx)
                .coerceIn(0f, currentMaxValue.toFloat())
            scrollPosition.snapTo(target)
            val nearest = target.roundToInt()
            if (nearest != currentValue) {
                currentOnSelect(nearest)
            }
        }
    }

    LaunchedEffect(value, maxValue) {
        if (!isDragging && scrollPosition.value.roundToInt() != safeValue) {
            scrollPosition.animateTo(safeValue.toFloat())
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .height(NumberWheelItemHeight * NumberWheelVisibleItemCount)
            .clip(wheelShape)
            .background(themeContainerColor())
            .border(width = 1.dp, color = themeContainerBorderColor(), shape = wheelShape)
            .draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                enabled = enabled,
                onDragStarted = { isDragging = true },
                onDragStopped = { velocity ->
                    isDragging = false
                    scope.launch {
                        val projected = scrollPosition.value - velocity / itemHeightPx * 0.08f
                        val nearest = projected.roundToInt().coerceIn(0, currentMaxValue)
                        scrollPosition.animateTo(nearest.toFloat())
                        currentOnSelect(nearest)
                    }
                },
            ),
    ) {
        val containerHeightPx = constraints.maxHeight.toFloat()
        val containerCenterY = containerHeightPx / 2f

        Box(
            modifier = Modifier
                .clipToBounds()
                .matchParentSize(),
        ) {
            (0..currentMaxValue).forEach { index ->
                val centerY = containerCenterY +
                    (index - scrollPosition.value) * itemHeightPx
                val topY = centerY - itemHeightPx / 2f
                if (topY > -itemHeightPx && topY < containerHeightPx) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .offset { IntOffset(x = 0, y = topY.roundToInt()) }
                            .fillMaxWidth()
                            .height(NumberWheelItemHeight),
                    ) {
                        Text(
                            text = index.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.bodyLarge,
                            color = contentColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 5.dp)
                .fillMaxWidth()
                .height(NumberWheelItemHeight)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(8.dp),
                ),
        )
    }
}
