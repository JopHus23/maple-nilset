package com.mapherdf.maplenilset.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import com.mapherdf.maplenilset.R

private val ScheduleActionButtonSize = 40.dp
private val ScheduleActionSpacing = 8.dp
private val ScheduleActionGap = 8.dp

@Composable
fun ScheduleCourseActionMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val popupOffsetY = with(LocalDensity.current) {
        -(ScheduleActionGap + ScheduleActionButtonSize).toPx().roundToInt()
    }

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(x = 0, y = popupOffsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(ScheduleActionSpacing)) {
            ScheduleCourseActionButton(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                iconRes = R.drawable.ic_edit,
                contentDescription = stringResource(R.string.schedule_course_edit),
                onClick = onEdit,
            )
            ScheduleCourseActionButton(
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                iconRes = R.drawable.ic_delete,
                contentDescription = stringResource(R.string.schedule_course_delete),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun ScheduleCourseActionButton(
    color: Color,
    contentColor: Color,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.size(ScheduleActionButtonSize),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
            )
        }
    }
}
