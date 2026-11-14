package com.mapherdf.maplenilset.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.core.notification.NotificationSettings
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val themeSettings by settingsViewModel.themeSettings.collectAsStateWithLifecycle()
    val showBorder = themeSettings.showCardBorders
    var canNotify by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    var reminderInput by remember(settings.reminderLeadMinutes) {
        mutableStateOf(settings.reminderLeadMinutes.toString())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        canNotify = granted
        if (granted) {
            viewModel.setScheduleNotificationsEnabled(true)
        } else {
            viewModel.setScheduleNotificationsEnabled(false)
        }
    }

    Scaffold(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                    softwareKeyboardController?.hide()
                }
            },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notification_settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeContainerColor(),
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NotificationToggleCard(
                title = stringResource(R.string.notification_master_title),
                summary = if (canNotify) {
                    stringResource(R.string.notification_permission_granted)
                } else {
                    stringResource(R.string.notification_permission_denied)
                },
                iconRes = R.drawable.ic_notification,
                checked = settings.scheduleNotificationsEnabled,
                showBorder = showBorder,
                onCheckedChange = { enabled ->
                    if (enabled && !canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setScheduleNotificationsEnabled(enabled)
                    }
                },
            )

            NotificationToggleCard(
                title = stringResource(R.string.notification_persistent_setting),
                summary = stringResource(R.string.notification_persistent_setting_summary),
                iconRes = R.drawable.ic_schedule,
                checked = settings.persistentCourseEnabled,
                enabled = settings.scheduleNotificationsEnabled,
                showBorder = showBorder,
                onCheckedChange = viewModel::setPersistentCourseEnabled,
            )

            NotificationReminderCard(
                settings = settings,
                enabled = settings.scheduleNotificationsEnabled,
                reminderInput = reminderInput,
                showBorder = showBorder,
                onCheckedChange = viewModel::setReminderEnabled,
                onReminderInputChange = { value ->
                    reminderInput = value.filter { it.isDigit() }.take(5)
                    reminderInput.toIntOrNull()?.let(viewModel::setReminderLeadMinutes)
                },
                onSaveReminderInput = {
                    val minutes = reminderInput.toIntOrNull()
                    if (minutes == null || minutes !in NotificationSettings.MIN_REMINDER_LEAD_MINUTES..NotificationSettings.MAX_REMINDER_LEAD_MINUTES) {
                        reminderInput = settings.reminderLeadMinutes.toString()
                    } else {
                        viewModel.setReminderLeadMinutes(minutes)
                    }
                },
            )

            NotificationContentCard(
                settings = settings,
                enabled = settings.scheduleNotificationsEnabled,
                showBorder = showBorder,
                onClassroomChange = viewModel::setShowClassroom,
                onTeacherChange = viewModel::setShowTeacher,
                onNoteChange = viewModel::setShowNote,
            )

            NotificationDiagnosticsCard(
                canNotify = canNotify,
                showBorder = showBorder,
                onSendTest = {
                    if (canNotify) {
                        viewModel.sendTestNotification()
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenExactAlarmSettings = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun NotificationToggleCard(
    title: String,
    summary: String,
    iconRes: Int,
    checked: Boolean,
    showBorder: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    NotificationCard(showBorder = showBorder) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(summary) },
            leadingContent = {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = onCheckedChange,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!checked) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun NotificationReminderCard(
    settings: NotificationSettings,
    enabled: Boolean,
    reminderInput: String,
    showBorder: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onReminderInputChange: (String) -> Unit,
    onSaveReminderInput: () -> Unit,
) {
    val inputError = reminderInput.isNotEmpty() &&
        reminderInput.toIntOrNull() !in NotificationSettings.MIN_REMINDER_LEAD_MINUTES..NotificationSettings.MAX_REMINDER_LEAD_MINUTES
    NotificationCard(showBorder = showBorder) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.notification_reminder_setting)) },
            supportingContent = { Text(stringResource(R.string.notification_reminder_setting_summary)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_event),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = settings.reminderEnabled,
                    enabled = enabled,
                    onCheckedChange = onCheckedChange,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onCheckedChange(!settings.reminderEnabled) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
        HorizontalDivider(color = themeContainerBorderColor().copy(alpha = 0.45f))
        OutlinedTextField(
            value = reminderInput,
            onValueChange = onReminderInputChange,
            enabled = enabled,
            isError = inputError,
            label = { Text(stringResource(R.string.notification_reminder_minutes_label)) },
            supportingText = {
                Text(
                    text = if (inputError) {
                        stringResource(R.string.notification_reminder_invalid)
                    } else {
                        stringResource(R.string.notification_reminder_range)
                    },
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state ->
                    if (!state.isFocused) onSaveReminderInput()
                },
        )
    }
}

@Composable
private fun NotificationContentCard(
    settings: NotificationSettings,
    enabled: Boolean,
    showBorder: Boolean,
    onClassroomChange: (Boolean) -> Unit,
    onTeacherChange: (Boolean) -> Unit,
    onNoteChange: (Boolean) -> Unit,
) {
    NotificationCard(
        title = stringResource(R.string.notification_content_title),
        summary = stringResource(R.string.notification_content_summary),
        showBorder = showBorder,
    ) {
        NotificationCheckRow(
            title = stringResource(R.string.notification_show_classroom),
            iconRes = R.drawable.ic_schedule,
            checked = settings.showClassroom,
            enabled = enabled,
            onChange = onClassroomChange,
        )
        NotificationCheckRow(
            title = stringResource(R.string.notification_show_teacher),
            iconRes = R.drawable.ic_account,
            checked = settings.showTeacher,
            enabled = enabled,
            onChange = onTeacherChange,
        )
        NotificationCheckRow(
            title = stringResource(R.string.notification_show_note),
            iconRes = R.drawable.ic_note,
            checked = settings.showNote,
            enabled = enabled,
            onChange = onNoteChange,
        )
    }
}

@Composable
private fun NotificationDiagnosticsCard(
    canNotify: Boolean,
    showBorder: Boolean,
    onSendTest: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    val context = LocalContext.current
    val exactGranted = remember {
        val alarmManager = androidx.core.content.ContextCompat.getSystemService(
            context,
            android.app.AlarmManager::class.java,
        )
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager?.canScheduleExactAlarms() == true
    }
    NotificationCard(
        title = stringResource(R.string.notification_diagnostics_title),
        summary = stringResource(R.string.notification_diagnostics_summary),
        showBorder = showBorder,
    ) {
        NotificationActionRow(
            title = stringResource(R.string.notification_test),
            enabled = canNotify,
            onClick = onSendTest,
        )
        if (!exactGranted) {
            NotificationActionRow(
                title = stringResource(R.string.notification_exact_alarm_open_settings),
                enabled = true,
                onClick = onOpenExactAlarmSettings,
            )
            Text(
                text = stringResource(R.string.notification_exact_alarm_fallback),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun NotificationCheckRow(
    title: String,
    iconRes: Int,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
            color = if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_tick),
            contentDescription = null,
            tint = if (checked) {
                MaterialTheme.colorScheme.primary
            } else {
                Color.Transparent
            },
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun NotificationActionRow(
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun NotificationCard(
    title: String? = null,
    summary: String? = null,
    showBorder: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = if (showBorder) {
            BorderStroke(1.dp, themeContainerBorderColor())
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            summary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}
