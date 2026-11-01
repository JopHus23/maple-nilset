package com.mapherdf.maplenilset.ui.color

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.data.color.ColorConverter
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorAddScreen(
    editId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ColorViewModel = hiltViewModel(),
) {
    var selectedFormat by remember { mutableStateOf(ColorFormat.HEX) }
    var inputValue by remember { mutableStateOf("") }
    var colorName by remember { mutableStateOf("") }
    var originalFormat by remember { mutableStateOf<ColorFormat?>(null) }
    var originalValue by remember { mutableStateOf("") }
    var didLoadEdit by rememberSaveable { mutableStateOf(false) }

    val parseResult = remember(inputValue, selectedFormat) {
        if (inputValue.isBlank()) null else viewModel.parseColor(selectedFormat, inputValue)
    }
    val savedColors by viewModel.savedColors.collectAsStateWithLifecycle()
    val isSavingColor by viewModel.isSavingColor.collectAsStateWithLifecycle()
    val editingColor = editId?.let { id -> savedColors.find { it.id == id } }
    val rgb = parseResult?.rgb
    val nameEntry = rgb?.let { viewModel.colorName(it) }

    LaunchedEffect(editingColor) {
        val color = editingColor ?: return@LaunchedEffect
        if (didLoadEdit) return@LaunchedEffect
        val savedFormat = runCatching { ColorFormat.valueOf(color.sourceType) }.getOrDefault(ColorFormat.HEX)
        val sourceFormat = if (savedFormat == ColorFormat.NCS) ColorFormat.HEX else savedFormat
        val sourceRgb = ColorConverter.hexToRgb(color.hex)
        selectedFormat = sourceFormat
        inputValue = when {
            sourceFormat == ColorFormat.HEX -> sourceRgb?.let { ColorConverter.rgbToHex(it) }.orEmpty()
            else -> color.sourceValue ?: sourceRgb?.let { viewModel.formatForRgb(sourceFormat, it) }.orEmpty()
        }
        colorName = color.name
        didLoadEdit = true
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (editId != null) R.string.lienzo_color_edit else R.string.lienzo_color_add)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeContainerColor()),
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
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ColorFormatTabs(
                selected = selectedFormat,
                onSelect = { format ->
                    val currentResult = if (inputValue.isNotBlank()) viewModel.parseColor(selectedFormat, inputValue) else null
                    if (currentResult != null) {
                        if (originalFormat == null) {
                            originalFormat = selectedFormat
                            originalValue = inputValue
                        }
                        if (format == originalFormat) {
                            inputValue = originalValue
                        } else {
                            inputValue = viewModel.formatForRgb(format, currentResult.rgb)
                        }
                    }
                    selectedFormat = format
                },
            )

            Text(
                text = formatDescription(selectedFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = inputValue,
                onValueChange = { value ->
                    inputValue = value
                    val typedResult = viewModel.parseColor(selectedFormat, value)
                    if (typedResult != null && (originalFormat != selectedFormat || value != originalValue)) {
                        originalFormat = selectedFormat
                        originalValue = value
                    }
                },
                label = { Text(stringResource(R.string.lienzo_color_input_hex)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
            Text(
                text = formatHint(selectedFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            rgb?.let { Color(it.r, it.g, it.b) } ?: MaterialTheme.colorScheme.surfaceVariant,
                        )
                        .border(BorderStroke(1.dp, themeContainerBorderColor()), RoundedCornerShape(8.dp)),
                )
                if (nameEntry != null) {
                    Column {
                        Text(
                            text = nameEntry.chinese,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = nameEntry.english,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = colorName,
                onValueChange = { colorName = it },
                label = { Text(stringResource(R.string.lienzo_color_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )

            Button(
                onClick = {
                    val result = viewModel.parseColor(selectedFormat, inputValue) ?: return@Button
                    viewModel.saveColor(
                        editId = editId,
                        name = colorName,
                        rgb = result.rgb,
                        format = selectedFormat,
                        inputValue = inputValue,
                        onSaved = onNavigateBack,
                    )
                },
                enabled = parseResult != null && !isSavingColor,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.lienzo_color_save))
            }
        }
    }
}

@Composable
private fun ColorFormatTabs(
    selected: ColorFormat,
    onSelect: (ColorFormat) -> Unit,
) {
    val row1 = listOf(ColorFormat.HEX, ColorFormat.RGB, ColorFormat.CMYK)
    val row2 = listOf(ColorFormat.HSL, ColorFormat.HSV_HSB, ColorFormat.LAB)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { format ->
                FormatTab(format, format == selected, Modifier.weight(1f), onSelect)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row2.forEach { format ->
                FormatTab(format, format == selected, Modifier.weight(1f), onSelect)
            }
        }
    }
}

@Composable
private fun FormatTab(
    format: ColorFormat,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (ColorFormat) -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    Color.Transparent
                },
            )
            .clickable { onSelect(format) }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = format.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun formatDescription(format: ColorFormat): String = stringResource(
    when (format) {
        ColorFormat.HEX -> R.string.lienzo_color_desc_hex
        ColorFormat.RGB -> R.string.lienzo_color_desc_rgb
        ColorFormat.CMYK -> R.string.lienzo_color_desc_cmyk
        ColorFormat.HSL -> R.string.lienzo_color_desc_hsl
        ColorFormat.HSV_HSB -> R.string.lienzo_color_desc_hsv
        ColorFormat.NCS -> R.string.lienzo_color_desc_ncs
        ColorFormat.LAB -> R.string.lienzo_color_desc_lab
    },
)

@Composable
private fun formatHint(format: ColorFormat): String = stringResource(
    when (format) {
        ColorFormat.HEX -> R.string.lienzo_color_input_hex_hint
        ColorFormat.RGB -> R.string.lienzo_color_input_rgb_hint
        ColorFormat.CMYK -> R.string.lienzo_color_input_cmyk_hint
        ColorFormat.HSL -> R.string.lienzo_color_input_hsl_hint
        ColorFormat.HSV_HSB -> R.string.lienzo_color_input_hsv_hint
        ColorFormat.NCS -> R.string.lienzo_color_input_ncs_hint
        ColorFormat.LAB -> R.string.lienzo_color_input_lab_hint
    },
)
