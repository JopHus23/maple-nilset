package com.mapherdf.maplenilset.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.core.theme.ThemeMode
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenConsole: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenThemeSettings: () -> Unit,
    onOpenCustomSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeSettings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val modeLabel = stringResource(
        when (themeSettings.mode) {
            ThemeMode.LIGHT -> R.string.theme_mode_light
            ThemeMode.DARK -> R.string.theme_mode_dark
        },
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeContainerColor(),
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsNavigationCard(
                title = stringResource(R.string.settings_open_console),
                iconRes = R.drawable.ic_console,
                showBorder = themeSettings.showCardBorders,
                onClick = onOpenConsole,
            )
            SettingsNavigationCard(
                title = stringResource(R.string.settings_notification),
                iconRes = R.drawable.ic_notification,
                showBorder = themeSettings.showCardBorders,
                onClick = onOpenNotificationSettings,
            )
            Surface(
                onClick = onOpenThemeSettings,
                shape = RoundedCornerShape(8.dp),
                border = if (themeSettings.showCardBorders) {
                    BorderStroke(1.dp, themeContainerBorderColor())
                } else {
                    null
                },
                color = themeContainerColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme)) },
                    supportingContent = {
                        Text(
                            text = buildString {
                                append(themeSettings.palette.label)
                                append(" · ")
                                append(modeLabel)
                            },
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_theme),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            SettingsNavigationCard(
                title = stringResource(R.string.settings_custom),
                iconRes = R.drawable.ic_customize,
                showBorder = themeSettings.showCardBorders,
                onClick = onOpenCustomSettings,
            )
            SettingsNavigationCard(
                title = stringResource(R.string.settings_about),
                iconRes = R.drawable.ic_info,
                showBorder = themeSettings.showCardBorders,
                onClick = onOpenAbout,
            )
        }
    }
}

@Composable
private fun SettingsNavigationCard(
    title: String,
    iconRes: Int,
    showBorder: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = if (showBorder) {
            BorderStroke(1.dp, themeContainerBorderColor())
        } else {
            null
        },
        color = themeContainerColor(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            leadingContent = {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
