package com.mapherdf.maplenilset.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.ui.theme.themeDrawerMaskColor

private val editorWeekdayTitles = listOf(
    R.string.calendar_weekday_monday,
    R.string.calendar_weekday_tuesday,
    R.string.calendar_weekday_wednesday,
    R.string.calendar_weekday_thursday,
    R.string.calendar_weekday_friday,
    R.string.calendar_weekday_saturday,
    R.string.calendar_weekday_sunday,
)

private val editorWeekdayValues = listOf(1, 2, 3, 4, 5, 6, 0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCourseEditor(
    state: ScheduleUiState,
    onDismiss: () -> Unit,
    onSubmit: (ScheduleCourseDraft) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editingCourse = state.editingCourse
    var title by remember(editingCourse) { mutableStateOf(editingCourse?.title.orEmpty()) }
    var weekday by remember(editingCourse) {
        mutableStateOf(editingCourse?.weekday ?: state.selectedWeekday)
    }
    var startHour by remember(editingCourse) { mutableStateOf((editingCourse?.startMin ?: 480) / 60) }
    var startMinute by remember(editingCourse) { mutableStateOf((editingCourse?.startMin ?: 480) % 60) }
    var endHour by remember(editingCourse) { mutableStateOf((editingCourse?.endMin ?: 540) / 60) }
    var endMinute by remember(editingCourse) { mutableStateOf((editingCourse?.endMin ?: 540) % 60) }
    var teacher by remember(editingCourse) { mutableStateOf(editingCourse?.teacher.orEmpty()) }
    var classroom by remember(editingCourse) { mutableStateOf(editingCourse?.classroom.orEmpty()) }
    var note by remember(editingCourse) { mutableStateOf(editingCourse?.note.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!state.isSavingCourse) onDismiss()
        },
        sheetState = sheetState,
        containerColor = themeDrawerMaskColor(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (editingCourse == null) {
                        R.string.schedule_create_course
                    } else {
                        R.string.schedule_edit_course
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.schedule_course_title)) },
                singleLine = true,
                enabled = !state.isSavingCourse,
                modifier = Modifier.fillMaxWidth(),
            )

            WeekdaySelector(
                weekday = weekday,
                enabled = !state.isSavingCourse,
                onSelect = { weekday = it },
            )

            TimeSelectionRow(
                hour = startHour,
                endHour = endHour,
                minute = startMinute,
                endMinute = endMinute,
                enabled = !state.isSavingCourse,
                onHourChange = { startHour = it },
                onMinuteChange = { startMinute = it },
                onEndHourChange = { endHour = it },
                onEndMinuteChange = { endMinute = it },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it },
                    label = { OptionalFieldLabel(R.string.schedule_course_teacher) },
                    singleLine = true,
                    enabled = !state.isSavingCourse,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = classroom,
                    onValueChange = { classroom = it },
                    label = { OptionalFieldLabel(R.string.schedule_course_classroom) },
                    singleLine = true,
                    enabled = !state.isSavingCourse,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { OptionalFieldLabel(R.string.schedule_course_note) },
                enabled = !state.isSavingCourse,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            state.courseEditorError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !state.isSavingCourse,
                ) {
                    Text(text = stringResource(R.string.schedule_cancel))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        onSubmit(
                            ScheduleCourseDraft(
                                title = title,
                                weekday = weekday,
                                startHour = startHour,
                                startMinute = startMinute,
                                endHour = endHour,
                                endMinute = endMinute,
                                teacher = teacher,
                                classroom = classroom,
                                note = note,
                            ),
                        )
                    },
                    enabled = !state.isSavingCourse,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (state.isSavingCourse) {
                                R.string.schedule_saving
                            } else {
                                R.string.schedule_save
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionalFieldLabel(titleRes: Int) {
    Text(
        text = buildAnnotatedString {
            append(stringResource(titleRes))
            append('\n')
            withStyle(
                style = SpanStyle(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                append(stringResource(R.string.schedule_optional))
            }
        },
        maxLines = 2,
        softWrap = true,
    )
}

@Composable
private fun WeekdaySelector(
    weekday: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val title = stringResource(
        editorWeekdayTitles.getOrElse(editorWeekdayValues.indexOf(weekday)) {
            editorWeekdayTitles.first()
        },
    )

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = title)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = themeDrawerMaskColor(),
        ) {
            editorWeekdayTitles.forEachIndexed { index, titleRes ->
                DropdownMenuItem(
                    text = { Text(stringResource(titleRes)) },
                    onClick = {
                        onSelect(editorWeekdayValues[index])
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TimeSelectionRow(
    hour: Int,
    endHour: Int,
    minute: Int,
    endMinute: Int,
    enabled: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onEndHourChange: (Int) -> Unit,
    onEndMinuteChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.schedule_course_time),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TimeUnitSelector(
                value = hour,
                maxValue = 23,
                enabled = enabled,
                onSelect = onHourChange,
                modifier = Modifier.weight(1f),
            )
            TimeSeparator(text = ":")
            TimeUnitSelector(
                value = minute,
                maxValue = 59,
                enabled = enabled,
                onSelect = onMinuteChange,
                modifier = Modifier.weight(1f),
            )
            TimeSeparator(text = "-")
            TimeUnitSelector(
                value = endHour,
                maxValue = 23,
                enabled = enabled,
                onSelect = onEndHourChange,
                modifier = Modifier.weight(1f),
            )
            TimeSeparator(text = ":")
            TimeUnitSelector(
                value = endMinute,
                maxValue = 59,
                enabled = enabled,
                onSelect = onEndMinuteChange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TimeSeparator(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun TimeUnitSelector(
    value: Int,
    maxValue: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScheduleNumberWheel(
        value = value,
        maxValue = maxValue,
        enabled = enabled,
        onSelect = onSelect,
        modifier = modifier,
    )
}
