package com.mapherdf.maplenilset.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    val circleProgress = remember { Animatable(0f) }
    val slashProgress = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(1f) }
    val color = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        delay(300)
        circleProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = CubicBezierEasing(0.0f, 0.8f, 0.2f, 1.0f)),
        )
        slashProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = CubicBezierEasing(0.0f, 0.6f, 0.2f, 1.0f)),
        )
        delay(600)
        exitProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 500),
        )
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = exitProgress.value
                    scaleY = exitProgress.value
                    alpha = exitProgress.value
                },
        ) {
            val strokeWidth = 9.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = color,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                alpha = if (circleProgress.value < 1f) 0.15f else 1f,
            )

            val circleSweep = circleProgress.value
            if (circleSweep > 0f) {
                val startAngle = -90f
                val sweepAngle = 360f * circleSweep
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            val slash = slashProgress.value
            if (slash > 0f) {
                val slashAngle = -45f
                val slashLength = radius * 1.35f
                val dx = cos(Math.toRadians(slashAngle.toDouble())).toFloat()
                val dy = sin(Math.toRadians(slashAngle.toDouble())).toFloat()

                val endOffset = Offset(
                    x = center.x + dx * slashLength * slash,
                    y = center.y + dy * slashLength * slash,
                )
                val startOffset = Offset(
                    x = center.x - dx * slashLength * slash,
                    y = center.y - dy * slashLength * slash,
                )

                val path = Path().apply {
                    moveTo(startOffset.x, startOffset.y)
                    lineTo(endOffset.x, endOffset.y)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
        }
    }
}
