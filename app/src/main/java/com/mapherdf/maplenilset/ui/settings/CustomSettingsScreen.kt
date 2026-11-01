package com.mapherdf.maplenilset.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.core.navigation.HomeDestination
import com.mapherdf.maplenilset.core.navigation.HomeDestinationMove
import com.mapherdf.maplenilset.ui.theme.themeContainerColor
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CustomSettingsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiSettings by viewModel.uiSettings.collectAsStateWithLifecycle()
    val themeSettings by settingsViewModel.themeSettings.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_settings_title)) },
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
            StartupPageCard(
                pages = uiSettings.visiblePages,
                startupPage = uiSettings.startupPage,
                showBorder = themeSettings.showCardBorders,
                onSelect = viewModel::selectStartupPage,
            )

            SidebarCustomizationCard(
                pages = uiSettings.pageOrder,
                hiddenPages = uiSettings.hiddenPages,
                visibleCount = uiSettings.visiblePages.size,
                showBorder = themeSettings.showCardBorders,
                onMove = viewModel::movePage,
                onSetVisible = viewModel::setPageVisible,
            )

            SettingsActionCard(
                title = stringResource(R.string.custom_settings_reset_title),
                iconRes = R.drawable.ic_reset,
                showBorder = themeSettings.showCardBorders,
                onClick = { showResetDialog = true },
            )
        }
    }

    if (showResetDialog) {
        val title = stringResource(R.string.custom_settings_reset_title)
        val message = stringResource(R.string.custom_settings_reset_message)
        val confirm = stringResource(R.string.custom_settings_reset_confirm)
        val cancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.resetUiSettings()
                    },
                ) { Text(confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(cancel) }
            },
        )
    }
}

@Composable
private fun StartupPageCard(
    pages: List<HomeDestination>,
    startupPage: HomeDestination,
    showBorder: Boolean,
    onSelect: (HomeDestination) -> Unit,
) {
    SettingsGroupCard(
        title = stringResource(R.string.custom_settings_startup_title),
        summary = stringResource(R.string.custom_settings_startup_summary),
        showBorder = showBorder,
    ) {
        pages.forEach { page ->
            val isSelected = page == startupPage
            val title = stringResource(page.titleRes())
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(page) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(page.iconRes()),
                    contentDescription = null,
                    tint = if (isSelected) {
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
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tick),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarCustomizationCard(
    pages: List<HomeDestination>,
    hiddenPages: Set<HomeDestination>,
    visibleCount: Int,
    showBorder: Boolean,
    onMove: (HomeDestination, HomeDestinationMove) -> Unit,
    onSetVisible: (HomeDestination, Boolean) -> Unit,
) {
    SettingsGroupCard(
        title = stringResource(R.string.custom_settings_sidebar_title),
        summary = stringResource(R.string.custom_settings_sidebar_summary),
        showBorder = showBorder,
    ) {
        pages.forEachIndexed { index, page ->
            if (index > 0) {
                HorizontalDivider(
                    color = themeContainerBorderColor().copy(alpha = 0.45f),
                )
            }
            val isVisible = page !in hiddenPages
            val rowAlpha = if (isVisible) 1f else 0.48f
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .alpha(rowAlpha),
            ) {
                Icon(
                    painter = painterResource(page.iconRes()),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = stringResource(page.titleRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                UiSettingsIconButton(
                    iconRes = R.drawable.ic_arrow_upward,
                    contentDescription = stringResource(R.string.custom_settings_move_up),
                    enabled = index > 0,
                    onClick = { onMove(page, HomeDestinationMove.UP) },
                )
                UiSettingsIconButton(
                    iconRes = R.drawable.ic_arrow_downward,
                    contentDescription = stringResource(R.string.custom_settings_move_down),
                    enabled = index < pages.lastIndex,
                    onClick = { onMove(page, HomeDestinationMove.DOWN) },
                )
                UiSettingsIconButton(
                    iconRes = if (isVisible) {
                        R.drawable.ic_visibility_on
                    } else {
                        R.drawable.ic_visibility_off
                    },
                    contentDescription = stringResource(
                        if (isVisible) {
                            R.string.custom_settings_hide_page
                        } else {
                            R.string.custom_settings_show_page
                        },
                    ),
                    enabled = isVisible || visibleCount > 1,
                    onClick = { onSetVisible(page, !isVisible) },
                )
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    summary: String,
    showBorder: Boolean,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.Surface(
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
private fun UiSettingsIconButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    iconRes: Int,
    showBorder: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = if (showBorder) {
            BorderStroke(1.dp, themeContainerBorderColor())
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun HomeDestination.titleRes(): Int = when (this) {
    HomeDestination.HOME -> R.string.drawer_home
    HomeDestination.CALENDAR -> R.string.drawer_calendar
    HomeDestination.NOTE -> R.string.drawer_note
    HomeDestination.TODO -> R.string.drawer_todo
    HomeDestination.SCHEDULE -> R.string.drawer_schedule
    HomeDestination.BILI -> R.string.drawer_bili_nil
    HomeDestination.LIENZO -> R.string.drawer_lienzo_color
}

private fun HomeDestination.iconRes(): Int = when (this) {
    HomeDestination.HOME -> R.drawable.ic_home
    HomeDestination.CALENDAR -> R.drawable.ic_event
    HomeDestination.NOTE -> R.drawable.ic_note
    HomeDestination.TODO -> R.drawable.ic_todo
    HomeDestination.SCHEDULE -> R.drawable.ic_schedule
    HomeDestination.BILI -> R.drawable.ic_bilibili
    HomeDestination.LIENZO -> R.drawable.ic_color
}
