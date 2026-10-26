package com.mapherdf.maplenilset.ui.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.core.theme.UserThemeSettings
import com.mapherdf.maplenilset.ui.settings.SettingsViewModel
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val hasCustomBackground = settings.backgroundImageUri != null

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.console_settings_title)) },
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, themeContainerBorderColor()),
                color = themeContainerColor(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.console_background))
                    },
                    supportingContent = if (hasCustomBackground) {
                        null
                    } else {
                        { Text(stringResource(R.string.console_background_disabled)) }
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_image),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.showConsoleBackground,
                            enabled = hasCustomBackground,
                            onCheckedChange = viewModel::setConsoleBackground,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = hasCustomBackground) {
                            viewModel.setConsoleBackground(!settings.showConsoleBackground)
                        },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            ConsoleFontSizeControl(
                fontSize = settings.effectiveConsoleOutputFontSize,
                onFontSizeChange = viewModel::setConsoleOutputFontSize,
            )
        }
    }
}

@Composable
private fun ConsoleFontSizeControl(
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
) {
    var sliderValue by remember(fontSize) {
        mutableFloatStateOf(fontSize)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, themeContainerBorderColor()),
        color = themeContainerColor(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.console_output_font_size))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_textscale),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    IconButton(
                        onClick = {
                            sliderValue = UserThemeSettings.DEFAULT_CONSOLE_OUTPUT_FONT_SIZE
                            onFontSizeChange(sliderValue)
                        },
                        modifier = Modifier.padding(0.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_reset),
                            contentDescription = stringResource(R.string.console_reset_font_size),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    sliderValue = value
                },
                onValueChangeFinished = {
                    sliderValue = sliderValue.snapToConsoleFontSize()
                    onFontSizeChange(sliderValue)
                },
                valueRange = UserThemeSettings.MIN_CONSOLE_OUTPUT_FONT_SIZE..
                    UserThemeSettings.MAX_CONSOLE_OUTPUT_FONT_SIZE,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(
                    R.string.console_font_size_value,
                    sliderValue.roundToInt(),
                ),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 20.dp, bottom = 10.dp),
            )
        }
    }
}

private fun Float.snapToConsoleFontSize(): Float = roundToInt().toFloat()
