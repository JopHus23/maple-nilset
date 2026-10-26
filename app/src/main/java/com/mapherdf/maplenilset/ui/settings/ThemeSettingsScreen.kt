package com.mapherdf.maplenilset.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.core.theme.ThemeColorFields
import com.mapherdf.maplenilset.core.theme.ThemeColorParser
import com.mapherdf.maplenilset.core.theme.ThemeColors
import com.mapherdf.maplenilset.core.theme.ThemeMode
import com.mapherdf.maplenilset.core.theme.ThemePreset
import com.mapherdf.maplenilset.core.theme.UserThemeSettings
import androidx.compose.material3.TopAppBarDefaults
import com.mapherdf.maplenilset.ui.theme.themeContainerColor
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import kotlinx.coroutines.delay

private const val ThemeEntranceInputDelayMillis = 360L

private val colorLabels = mapOf(
    ThemeColorFields.PRIMARY to "primary（主按钮 / 选中态）",
    ThemeColorFields.SECONDARY to "secondary（点缀）",
    ThemeColorFields.SURFACE to "surface（文字 / 图标对比）",
)

private enum class ThemeClearTarget {
    Font,
    Background,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    onSelectBackgroundImage: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val useDarkTheme = settings.usesDarkTheme()
    var isInputEnabled by remember { mutableStateOf(false) }
    var clearTarget by remember { mutableStateOf<ThemeClearTarget?>(null) }

    LaunchedEffect(Unit) {
        delay(ThemeEntranceInputDelayMillis)
        isInputEnabled = true
    }

    Scaffold(
        modifier = Modifier.blockTouchWhileEntranceLocked(!isInputEnabled),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_settings_title)) },
               colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeContainerColor(),
               ),
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp),
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemeModeGroup(
                settings = settings,
                onModeChange = viewModel::setThemeMode,
            )
            ThemeCardBorderToggle(
                settings = settings,
                onChange = viewModel::setCardBorders,
            )
            ThemeCardOpacityControl(
                opacity = settings.cardMaskOpacity,
                showBorder = settings.showCardBorders,
                onOpacityChange = viewModel::setCardMaskOpacity,
                onReset = {
                    viewModel.setCardMaskOpacity(UserThemeSettings.DEFAULT_CARD_MASK_OPACITY)
                },
            )

            Text(
                text = stringResource(R.string.theme_palette),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemePreset.entries.chunked(2).forEach { rowPalettes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowPalettes.forEach { palette ->
                        ThemePaletteCard(
                            palette = palette,
                            useDark = useDarkTheme,
                            isSelected = settings.palette == palette,
                            showBorder = settings.showCardBorders,
                            customColors = if (palette == ThemePreset.CUSTOM) {
                                if (useDarkTheme) settings.customDarkColors else settings.customLightColors
                            } else {
                                null
                            },
                            onClick = { viewModel.selectPalette(palette.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowPalettes.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            if (settings.palette == ThemePreset.CUSTOM) {
                ThemeCustomColorEditor(
                    settings = settings,
                    useDark = useDarkTheme,
                    showBorder = settings.showCardBorders,
                    onSaveColors = { values ->
                        viewModel.saveCustomColors(values, useDarkTheme)
                    },
                    onResetColors = {
                        viewModel.resetCustomColors(useDarkTheme)
                    },
                )
            }

            Text(
                text = stringResource(R.string.theme_display_scaling),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemeScaleControl(
                title = stringResource(R.string.theme_text_scale),
                iconRes = R.drawable.ic_textscale,
                checked = settings.textScaleEnabled,
                scale = settings.textScale,
                showBorder = settings.showCardBorders,
                onCheckedChange = viewModel::setTextScaleEnabled,
                onScaleChange = viewModel::setTextScale,
                onReset = {
                    viewModel.setTextScaleEnabled(false)
                    viewModel.setTextScale(UserThemeSettings.DEFAULT_SCALE)
                },
            )
            ThemeScaleControl(
                title = stringResource(R.string.theme_ui_scale),
                iconRes = R.drawable.ic_uiscale,
                checked = settings.uiScaleEnabled,
                scale = settings.uiScale,
                showBorder = settings.showCardBorders,
                onCheckedChange = viewModel::setUiScaleEnabled,
                onScaleChange = viewModel::setUiScale,
                onReset = {
                    viewModel.setUiScaleEnabled(false)
                    viewModel.setUiScale(UserThemeSettings.DEFAULT_SCALE)
                },
            )
            ThemeCustomFontCard(
                settings = settings,
                showBorder = settings.showCardBorders,
                cardMaskOpacity = settings.cardMaskOpacity,
                onFontSelected = viewModel::applyCustomFont,
                onRemoveFont = { clearTarget = ThemeClearTarget.Font },
            )
            ThemeCustomBackgroundCard(
                settings = settings,
                showBorder = settings.showCardBorders,
                cardMaskOpacity = settings.cardMaskOpacity,
                onImageSelected = onSelectBackgroundImage,
                onOpacityChange = viewModel::setBackgroundOpacity,
                onRemoveImage = { clearTarget = ThemeClearTarget.Background },
            )
        }
    }

    clearTarget?.let { target ->
        val title = stringResource(R.string.theme_clear_confirm_title)
        val message = stringResource(
            when (target) {
                ThemeClearTarget.Font -> R.string.theme_clear_font_message
                ThemeClearTarget.Background -> R.string.theme_clear_background_message
            },
        )
        val confirmLabel = stringResource(R.string.theme_clear_confirm)
        val cancelLabel = stringResource(R.string.cancel)

        AlertDialog(
            onDismissRequest = { clearTarget = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearTarget = null
                        when (target) {
                            ThemeClearTarget.Font -> viewModel.removeCustomFont()
                            ThemeClearTarget.Background -> viewModel.removeCustomBackgroundImage()
                        }
                    },
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearTarget = null }) {
                    Text(cancelLabel)
                }
            },
        )
    }
}

private fun Modifier.blockTouchWhileEntranceLocked(locked: Boolean): Modifier = pointerInput(locked) {
    if (!locked) return@pointerInput
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
                .changes
                .forEach { change -> change.consume() }
        }
    }
}

@Composable
private fun ThemeModeGroup(
    settings: UserThemeSettings,
    onModeChange: (ThemeMode) -> Unit,
) {
    val modes = listOf(
        ThemeMode.LIGHT to stringResource(R.string.theme_mode_light),
        ThemeMode.DARK to stringResource(R.string.theme_mode_dark),
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = if (settings.showCardBorders) {
            BorderStroke(1.dp, themeContainerBorderColor())
        } else {
            null
        },
    ) {
        Column {
            modes.forEachIndexed { index, (mode, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    supportingContent = null,
                    leadingContent = {
                        Icon(
                            painter = painterResource(
                                if (mode == ThemeMode.LIGHT) R.drawable.ic_bright else R.drawable.ic_dark,
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = settings.mode == mode,
                            onClick = null,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeChange(mode) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                if (index < modes.lastIndex) {
                    androidx.compose.material3.HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ThemeCardBorderToggle(
    settings: UserThemeSettings,
    onChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = if (settings.showCardBorders) {
            BorderStroke(1.dp, themeContainerBorderColor())
        } else {
            null
        },
    ) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.theme_card_borders)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_border),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingContent = {
                Switch(
                    checked = settings.showCardBorders,
                    onCheckedChange = onChange,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onChange(!settings.showCardBorders) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
private fun ThemeScaleControl(
    title: String,
    iconRes: Int,
    checked: Boolean,
    scale: Float,
    showBorder: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onScaleChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    var sliderValue by remember(checked, scale) {
        mutableStateOf(scale)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = if (showBorder) {
            BorderStroke(1.dp, themeContainerBorderColor())
        } else {
            null
        },
    ) {
        Column {
            ListItem(
                headlineContent = { Text(title) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_reset),
                                contentDescription = stringResource(R.string.theme_reset_scale),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Switch(
                            checked = checked,
                            onCheckedChange = onCheckedChange,
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
                    sliderValue = sliderValue.snapToPercentage()
                    onScaleChange(sliderValue)
                },
                valueRange = UserThemeSettings.MIN_SCALE..UserThemeSettings.MAX_SCALE,
                enabled = checked,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(
                    R.string.theme_scale_percent,
                    (sliderValue * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 20.dp, bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun ThemeCardOpacityControl(
    opacity: Float,
    showBorder: Boolean,
    onOpacityChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    var sliderValue by remember(opacity) {
        mutableStateOf(opacity)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = if (showBorder) {
            BorderStroke(1.dp, themeContainerBorderColor())
        } else {
            null
        },
    ) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.theme_card_opacity)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_opacity),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_reset),
                            contentDescription = stringResource(R.string.theme_reset_card_opacity),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
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
                    sliderValue = sliderValue.snapToPercentage()
                    onOpacityChange(sliderValue)
                },
                valueRange = UserThemeSettings.MIN_CARD_MASK_OPACITY..UserThemeSettings.MAX_CARD_MASK_OPACITY,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(
                    R.string.theme_card_opacity_percent,
                    (sliderValue * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 20.dp, bottom = 10.dp),
            )
        }
    }
}

private fun Float.snapToPercentage(): Float {
    return (this * 100).roundToInt() / 100f
}

@Composable
private fun ThemeCustomFontCard(
    settings: UserThemeSettings,
    showBorder: Boolean,
    cardMaskOpacity: Float,
    onFontSelected: (String) -> Unit,
    onRemoveFont: () -> Unit,
) {
    val fontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.toString()?.let(onFontSelected)
    }
    val currentFontName = settings.customFontName
        ?: settings.customFontPath
            ?.substringAfterLast('/')
            ?.substringBefore('?')
        ?: stringResource(R.string.theme_default_font)
    val hasCustomFont = settings.customFontPath != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = {
                fontPicker.launch(
                    arrayOf(
                        "*/*",
                    ),
                )
            },
            modifier = if (hasCustomFont) {
                Modifier.weight(1f)
            } else {
                Modifier.fillMaxWidth()
            },
            shape = RoundedCornerShape(8.dp),
            color = themeContainerColor(),
            border = if (showBorder) {
                BorderStroke(1.dp, themeContainerBorderColor())
            } else {
                null
            },
        ) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.theme_custom_font)) },
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_font),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_open),
                        contentDescription = stringResource(R.string.theme_open_font_picker),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }

        if (hasCustomFont) {
            Surface(
                onClick = onRemoveFont,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.error.copy(alpha = cardMaskOpacity),
                contentColor = MaterialTheme.colorScheme.onError,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cross),
                        contentDescription = stringResource(R.string.theme_reset_default_font),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }

    Text(
        text = stringResource(R.string.theme_current_font, currentFontName),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun ThemeCustomBackgroundCard(
    settings: UserThemeSettings,
    showBorder: Boolean,
    cardMaskOpacity: Float,
    onImageSelected: (String) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onRemoveImage: () -> Unit,
) {
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onImageSelected(uri.toString())
        }
    }
    var opacity by remember(settings.backgroundOpacity) {
        mutableStateOf(settings.backgroundOpacity)
    }
    val hasCustomBackgroundImage = settings.backgroundImageUri != null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                modifier = if (hasCustomBackgroundImage) {
                    Modifier.weight(1f)
                } else {
                    Modifier.fillMaxWidth()
                },
                shape = RoundedCornerShape(8.dp),
                color = themeContainerColor(),
                border = if (showBorder) {
                    BorderStroke(1.dp, themeContainerBorderColor())
                } else {
                    null
                },
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.theme_custom_background)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_image),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_open),
                            contentDescription = stringResource(R.string.theme_open_background_picker),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            if (hasCustomBackgroundImage) {
                Surface(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = cardMaskOpacity),
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cross),
                            contentDescription = stringResource(R.string.theme_remove_background_image),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        Slider(
            value = opacity,
            onValueChange = { value -> opacity = value },
            onValueChangeFinished = {
                opacity = opacity.snapToPercentage()
                onOpacityChange(opacity)
            },
            valueRange = UserThemeSettings.MIN_BACKGROUND_OPACITY..UserThemeSettings.MAX_BACKGROUND_OPACITY,
            enabled = settings.backgroundImageUri != null,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(
                R.string.theme_background_opacity,
                (opacity * 100).roundToInt(),
            ),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun ThemePaletteCard(
    palette: ThemePreset,
    useDark: Boolean,
    isSelected: Boolean,
    showBorder: Boolean,
    customColors: ThemeColors?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = if (showBorder) {
            BorderStroke(
                width = 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    themeContainerBorderColor()
                },
            )
        } else {
            null
        },
        color = themeContainerColor(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemePalettePreview(palette, useDark, customColors)
            Text(
                text = palette.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
private fun ThemePalettePreview(
    palette: ThemePreset,
    useDark: Boolean,
    customColors: ThemeColors?,
) {
    val colors = palette.colors(useDark)
        ?: customColors
        ?: UserThemeSettings.FALLBACK_DARK_COLORS
    val swatches = ThemeColorFields.EDITABLE.mapNotNull { field ->
        ThemeColorParser.parseArgb(colors.value(field))?.let(::Color)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        swatches.forEach { color ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun ThemeCustomColorEditor(
    settings: UserThemeSettings,
    useDark: Boolean,
    showBorder: Boolean,
    onSaveColors: (Map<String, String>) -> Unit,
    onResetColors: () -> Unit,
) {
    var colorValues by remember(
        settings.mode,
        settings.customLightColors,
        settings.customDarkColors,
    ) {
        mutableStateOf(
            settings.effectiveColors(useDark).let { colors ->
                ThemeColorFields.EDITABLE.associateWith { field ->
                    colors.value(field).orEmpty()
                }
            },
        )
    }
    val canSave = colorValues.values.all { value ->
        ThemeColorParser.normalize(value) != null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = if (showBorder) {
            BorderStroke(1.dp, themeContainerBorderColor())
        } else {
            null
        },
        color = themeContainerColor(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeColorFields.EDITABLE.forEach { field ->
                val value = colorValues.getValue(field)
                val parsedColor = ThemeColorParser.parseArgb(value)
                val isError = value.isNotBlank() && parsedColor == null

                OutlinedTextField(
                    value = value,
                    onValueChange = { next ->
                        colorValues = colorValues + (field to next)
                    },
                    label = { Text(colorLabels.getValue(field)) },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(stringResource(R.string.theme_invalid_color)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    parsedColor?.let(::Color)
                                        ?: MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { onSaveColors(colorValues) },
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.theme_save_colors))
                }
                OutlinedButton(
                    onClick = onResetColors,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.theme_reset_colors))
                }
            }
        }
    }
}
