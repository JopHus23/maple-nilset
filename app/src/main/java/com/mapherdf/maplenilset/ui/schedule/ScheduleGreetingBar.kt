package com.mapherdf.maplenilset.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@Composable
fun ScheduleGreetingBar(
    state: ScheduleUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
        color = themeContainerColor(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.schedule_greeting,
                        ScheduleCourseSelector.greetingFor(state.greetingHour),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = nextCourseText(state.nextCourse),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun nextCourseText(course: ScheduleNextCourse): String = when (course.kind) {
    ScheduleNextCourseKind.TODAY -> stringResource(
        R.string.schedule_next_course_today,
        course.title.orEmpty(),
        course.startTime.orEmpty(),
        course.endTime.orEmpty(),
    )
    ScheduleNextCourseKind.TOMORROW_FIRST -> stringResource(
        R.string.schedule_next_course_tomorrow,
        course.title.orEmpty(),
        course.startTime.orEmpty(),
        course.endTime.orEmpty(),
    )
    ScheduleNextCourseKind.TODAY_FINISHED -> stringResource(R.string.schedule_today_finished)
    ScheduleNextCourseKind.EMPTY -> stringResource(R.string.schedule_no_courses)
}
