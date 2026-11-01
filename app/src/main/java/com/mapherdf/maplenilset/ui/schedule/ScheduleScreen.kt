package com.mapherdf.maplenilset.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.data.calendar.CalendarItem
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteCourse by remember { mutableStateOf<CalendarItem?>(null) }

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScheduleGreetingBar(
                state = state,
            )

            ScheduleWeekdayTabs(
                selectedWeekday = state.selectedWeekday,
                todayWeekday = LocalDate.now().dayOfWeek.value % 7,
                onSelect = viewModel::selectWeekday,
            )

            Button(
                onClick = viewModel::showCourseEditor,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(text = stringResource(R.string.schedule_add_course))
            }

            state.errorMessage?.let { message ->
                ScheduleErrorCard(
                    message = message,
                    onRetry = viewModel::refresh,
                )
            }

            ScheduleCourseList(
                courses = state.selectedCourses,
                canModify = true,
                onEditCourse = viewModel::showEditCourse,
                onDeleteCourse = { course -> pendingDeleteCourse = course },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.isCourseEditorVisible) {
        ScheduleCourseEditor(
            state = state,
            onDismiss = viewModel::dismissCourseEditor,
            onSubmit = viewModel::saveCourse,
        )
    }

    pendingDeleteCourse?.let { course ->
        val title = stringResource(R.string.schedule_course_delete_title)
        val message = stringResource(R.string.schedule_course_delete_message)
        val confirm = stringResource(R.string.schedule_course_delete_confirm)
        val cancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = { pendingDeleteCourse = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteCourse = null
                        viewModel.deleteCourse(course)
                    },
                ) { Text(confirm) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCourse = null }) { Text(cancel) }
            },
        )
    }
}

@Composable
private fun ScheduleErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            themeContainerBorderColor(),
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Button(onClick = onRetry) {
                Text(text = stringResource(R.string.schedule_retry))
            }
        }
    }
}
