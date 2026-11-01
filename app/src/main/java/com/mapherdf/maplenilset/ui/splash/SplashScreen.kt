package com.mapherdf.maplenilset.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val circleProgress = remember { Animatable(0f) }
    val slashProgress = remember { Animatable(0f) }
    val exitProgress = remember { Animatable(1f) }
    val color = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

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
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = backgroundColor, alpha = exitProgress.value)

            scale(scale = exitProgress.value, pivot = center) {
                val strokeWidth = 9.dp.toPx()
                val radius = 60.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)

                val circleSweep = circleProgress.value
                if (circleSweep > 0f) {
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * circleSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        alpha = exitProgress.value,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )
                }

                val slash = slashProgress.value
                if (slash > 0f) {
                    val slashAngle = -45f
                    val slashLength = radius * 1.35f
                    val dx = cos(Math.toRadians(slashAngle.toDouble())).toFloat()
                    val dy = sin(Math.toRadians(slashAngle.toDouble())).toFloat()
                    val path = Path().apply {
                        moveTo(center.x - dx * slashLength * slash, center.y - dy * slashLength * slash)
                        lineTo(center.x + dx * slashLength * slash, center.y + dy * slashLength * slash)
                    }
                    drawPath(path, color, alpha = exitProgress.value, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                }
            }
        }
    }
}
