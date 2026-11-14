package com.mapherdf.maplenilset.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapherdf.maplenilset.core.notification.NotificationCoordinator
import com.mapherdf.maplenilset.core.notification.NotificationSettings
import com.mapherdf.maplenilset.data.repository.NotificationSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val notificationSettingsRepository: NotificationSettingsRepository,
    private val notificationCoordinator: NotificationCoordinator,
) : ViewModel() {

    val settings: StateFlow<NotificationSettings> = notificationSettingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettings(),
        )

    fun setScheduleNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsRepository.setScheduleNotificationsEnabled(enabled)
        }
    }

    fun setPersistentCourseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsRepository.setPersistentCourseEnabled(enabled)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsRepository.setReminderEnabled(enabled)
        }
    }

    fun setReminderLeadMinutes(minutes: Int) {
        viewModelScope.launch {
            notificationSettingsRepository.setReminderLeadMinutes(minutes)
        }
    }

    fun setShowClassroom(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsRepository.setShowClassroom(enabled)
        }
    }

    fun setShowTeacher(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsRepository.setShowTeacher(enabled)
        }
    }

    fun setShowNote(enabled: Boolean) {
        viewModelScope.launch {
            notificationSettingsRepository.setShowNote(enabled)
        }
    }

    fun sendTestNotification() {
        notificationCoordinator.postTestReminder()
    }
}
