package com.mapherdf.maplenilset.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.data.calendar.CalendarItem
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@Composable
fun ScheduleCourseList(
    courses: List<CalendarItem>,
    canModify: Boolean,
    onEditCourse: (CalendarItem) -> Unit,
    onDeleteCourse: (CalendarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (courses.isEmpty()) {
        ScheduleEmptyCard(
            text = stringResource(R.string.schedule_day_empty),
            modifier = modifier,
        )
        return
    }

    var selectedCourseIndex by remember { mutableStateOf(-1) }
    LaunchedEffect(courses) {
        selectedCourseIndex = -1
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
        color = themeContainerColor(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            courses.forEachIndexed { index, course ->
                if (index > 0) {
                    HorizontalDivider(
                        color = themeContainerBorderColor().copy(alpha = 0.45f),
                    )
                }
                if (canModify) {
                    LongPressCourseRow(
                        course = course,
                        isActionMenuVisible = selectedCourseIndex == index,
                        onLongPress = { selectedCourseIndex = index },
                        onDismiss = { selectedCourseIndex = -1 },
                        onEditCourse = onEditCourse,
                        onDeleteCourse = onDeleteCourse,
                    )
                } else {
                    ScheduleCourseRow(course = course)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LongPressCourseRow(
    course: CalendarItem,
    isActionMenuVisible: Boolean,
    onLongPress: () -> Unit,
    onDismiss: () -> Unit,
    onEditCourse: (CalendarItem) -> Unit,
    onDeleteCourse: (CalendarItem) -> Unit,
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onDismiss,
                    onLongClick = onLongPress,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScheduleCourseRowContent(
                course = course,
                modifier = Modifier.weight(1f),
            )
        }

        if (isActionMenuVisible) {
            ScheduleCourseActionMenu(
                onEdit = {
                    onDismiss()
                    onEditCourse(course)
                },
                onDelete = {
                    onDismiss()
                    onDeleteCourse(course)
                },
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun ScheduleCourseRow(course: CalendarItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScheduleCourseRowContent(
            course = course,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScheduleCourseRowContent(
    course: CalendarItem,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = course.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val details = listOfNotNull(
            course.teacher,
            course.classroom,
            course.note,
        ).joinToString(" · ")
        if (details.isNotEmpty()) {
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Text(
        text = "${ScheduleCourseSelector.formatTime(course.startMin)}-" +
            ScheduleCourseSelector.formatTime(course.endMin),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ScheduleEmptyCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
        color = themeContainerColor(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
