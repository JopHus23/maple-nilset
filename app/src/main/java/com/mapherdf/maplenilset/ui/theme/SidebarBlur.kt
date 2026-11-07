package com.mapherdf.maplenilset.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mapherdf.maplenilset.core.theme.SidebarBlurStyle

val SidebarBlurStyle.blurRadius: Dp
    get() = when (this) {
        SidebarBlurStyle.NONE -> 0.dp
        SidebarBlurStyle.MICA -> 12.dp
        SidebarBlurStyle.AERO -> 20.dp
        SidebarBlurStyle.ACRYLIC -> 28.dp
        SidebarBlurStyle.GAUSSIAN -> 40.dp
    }

@Composable
fun SidebarBlurDecoration(
    style: SidebarBlurStyle,
    modifier: Modifier = Modifier,
) {
    if (style == SidebarBlurStyle.NONE) return

    val decorationColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        when (style) {
            SidebarBlurStyle.NONE -> Unit
            SidebarBlurStyle.MICA -> drawRect(decorationColor.copy(alpha = 0.07f))
            SidebarBlurStyle.AERO -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.12f),
                        0.58f to Color.Transparent,
                        1f to Color.White.copy(alpha = 0.05f),
                    ),
                )
            }
            SidebarBlurStyle.ACRYLIC -> {
                drawRect(decorationColor.copy(alpha = 0.08f))

                val cellSize = 9.dp.toPx()
                val dotRadius = 1.dp.toPx() / 2f
                var positionY = 0f
                var rowIndex = 0
                while (positionY < size.height) {
                    var positionX = if (rowIndex % 2 == 0) 0f else cellSize / 2f
                    while (positionX < size.width) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.028f),
                            radius = dotRadius,
                            center = Offset(positionX, positionY),
                        )
                        positionX += cellSize
                    }
                    positionY += cellSize
                    rowIndex += 1
                }
            }
            SidebarBlurStyle.GAUSSIAN -> Unit
        }
    }
}

@Composable
fun SidebarBlurPreview(
    style: SidebarBlurStyle,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surface
    val sampleModifier = if (style == SidebarBlurStyle.NONE) {
        Modifier
    } else {
        Modifier.blur(
            radius = style.blurRadius,
            edgeTreatment = BlurredEdgeTreatment.Unbounded,
        )
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .then(sampleModifier)
                .background(surface)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(primary),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(secondary),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(secondary.copy(alpha = 0.62f)),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.52f)),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(secondary.copy(alpha = 0.34f)),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(themeSidebarSurfaceColor(style)),
        )
        SidebarBlurDecoration(
            style = style,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
