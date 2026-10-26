package com.mapherdf.maplenilset.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapherdf.maplenilset.data.calendar.CalendarItem
import com.mapherdf.maplenilset.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val localCalendarRepository: CalendarRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { loadCalendar() }
    }

    fun selectWeekday(weekday: Int) {
        if (weekday !in 0..6) return
        _uiState.update { state ->
            state.copy(
                selectedWeekday = weekday,
                selectedCourses = ScheduleCourseSelector.coursesFor(state.records, weekday),
            )
        }
    }

    fun showCourseEditor() {
        _uiState.update {
            it.copy(
                isCourseEditorVisible = true,
                editingCourse = null,
                courseEditorError = null,
            )
        }
    }

    fun showEditCourse(course: CalendarItem) {
        if (_uiState.value.isSavingCourse) return
        _uiState.update {
            it.copy(
                isCourseEditorVisible = true,
                editingCourse = course,
                courseEditorError = null,
            )
        }
    }

    fun dismissCourseEditor() {
        if (_uiState.value.isSavingCourse) return
        _uiState.update { it.copy(isCourseEditorVisible = false, courseEditorError = null) }
    }

    fun saveCourse(draft: ScheduleCourseDraft) {
        val current = _uiState.value
        if (current.isSavingCourse) return

        val normalizedTitle = draft.title.trim()
        val normalizedTeacher = draft.teacher.trim().takeIf { it.isNotEmpty() }
        val normalizedClassroom = draft.classroom.trim().takeIf { it.isNotEmpty() }
        val normalizedNote = draft.note.trim().takeIf { it.isNotEmpty() }
        val startMinute = draft.startHour * 60 + draft.startMinute
        val endMinute = draft.endHour * 60 + draft.endMinute
        val validationError = when {
            draft.startHour !in 0..23 || draft.startMinute !in 0..59 ||
                draft.endHour !in 0..23 || draft.endMinute !in 0..59 -> "请选择完整时间"
            draft.weekday !in 0..6 -> "请选择上课日"
            normalizedTitle.isEmpty() || normalizedTitle.length > 255 -> "请输入课程标题"
            endMinute <= startMinute -> "结束时间必须晚于开始时间"
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(courseEditorError = validationError) }
            return
        }

        val editingCourse = current.editingCourse
        val records = current.records
            .filterNot { editingCourse != null && it == editingCourse }
            .toMutableList()
        val newCourse = CalendarItem(
            weekday = draft.weekday,
            startMin = startMinute,
            endMin = endMinute,
            title = normalizedTitle,
            teacher = normalizedTeacher,
            classroom = normalizedClassroom,
            note = normalizedNote,
        )
        records.add(newCourse)

        _uiState.update { it.copy(isSavingCourse = true, courseEditorError = null) }
        viewModelScope.launch {
            localCalendarRepository.saveCalendar(records).fold(
                onSuccess = {
                    val now = LocalDateTime.now(clock)
                    val selectedWeekday = draft.weekday
                    _uiState.update { state ->
                        state.copy(
                            isSavingCourse = false,
                            isCourseEditorVisible = false,
                            editingCourse = null,
                            records = records,
                            selectedWeekday = selectedWeekday,
                            selectedCourses = ScheduleCourseSelector.coursesFor(records, selectedWeekday),
                            nextCourse = ScheduleCourseSelector.nextCourse(
                                records,
                                now.toLocalDate(),
                                toMinute(now),
                            ),
                            greetingHour = now.hour,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSavingCourse = false,
                            courseEditorError = error.message ?: "课程保存失败，请重试",
                        )
                    }
                },
            )
        }
    }

    fun deleteCourse(course: CalendarItem) {
        val current = _uiState.value
        if (current.isSavingCourse) return
        val records = current.records - course

        _uiState.update { it.copy(isSavingCourse = true) }
        viewModelScope.launch {
            localCalendarRepository.saveCalendar(records).fold(
                onSuccess = {
                    val now = LocalDateTime.now(clock)
                    val selectedWeekday = current.selectedWeekday
                    _uiState.update { state ->
                        state.copy(
                            isSavingCourse = false,
                            records = records,
                            selectedCourses = ScheduleCourseSelector.coursesFor(records, selectedWeekday),
                            nextCourse = ScheduleCourseSelector.nextCourse(
                                records,
                                now.toLocalDate(),
                                toMinute(now),
                            ),
                            greetingHour = now.hour,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isSavingCourse = false,
                            errorMessage = error.message ?: "课程删除失败，请重试",
                        )
                    }
                },
            )
        }
    }

    fun refresh() {
        viewModelScope.launch { loadCalendar(refreshing = true) }
    }

    private suspend fun loadCalendar(refreshing: Boolean = false) {
        _uiState.update { state ->
            state.copy(
                isRefreshing = refreshing,
                isLoading = !refreshing,
                errorMessage = null,
            )
        }

        val now = LocalDateTime.now(clock)
        localCalendarRepository.getCalendar().fold(
            onSuccess = { calendar ->
                val selectedWeekday = now.dayOfWeek.value % 7
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        records = calendar.records,
                        selectedWeekday = selectedWeekday,
                        selectedCourses = ScheduleCourseSelector.coursesFor(calendar.records, selectedWeekday),
                        nextCourse = ScheduleCourseSelector.nextCourse(
                            calendar.records,
                            now.toLocalDate(),
                            toMinute(now),
                        ),
                        greetingHour = now.hour,
                        errorMessage = null,
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = error.message ?: "课表加载失败，请重试",
                    )
                }
            },
        )
    }

    private fun toMinute(now: LocalDateTime): Int = now.hour * 60 + now.minute
}
