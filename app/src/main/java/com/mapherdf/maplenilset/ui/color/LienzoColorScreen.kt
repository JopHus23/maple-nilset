package com.mapherdf.maplenilset.ui.color

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.data.color.ColorConverter
import com.mapherdf.maplenilset.data.color.ColorName
import com.mapherdf.maplenilset.data.color.RgbColor
import com.mapherdf.maplenilset.data.color.SavedColor
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LienzoColorScreen(
    onAddColor: () -> Unit,
    onEditColor: (String) -> Unit,
    onViewColor: (String) -> Unit,
    viewModel: ColorViewModel = hiltViewModel(),
) {
    val savedColors by viewModel.savedColors.collectAsStateWithLifecycle()
    val copiedMessage = stringResource(R.string.lienzo_color_copied)
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var selectedColorId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable(onClick = onAddColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.lienzo_color_add),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
    ) { innerPadding ->
        if (savedColors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 56.dp),
                ) {
                    Text(
                        text = stringResource(R.string.lienzo_color_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "color_list_hint") {
                    Text(
                        text = stringResource(R.string.lienzo_color_list_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(savedColors, key = { it.id }) { savedColor ->
                    SavedColorCard(
                        savedColor = savedColor,
                        isActionMenuVisible = selectedColorId == savedColor.id,
                        onClick = {
                            selectedColorId = null
                            onViewColor(savedColor.id)
                        },
                        onLongClick = { selectedColorId = savedColor.id },
                        onDismissAction = { selectedColorId = null },
                        onEdit = {
                            selectedColorId = null
                            onEditColor(savedColor.id)
                        },
                        onDelete = {
                            selectedColorId = null
                            pendingDeleteId = savedColor.id
                        },
                        onCopy = {
                            clipboard.setText(AnnotatedString(viewModel.colorInfo(savedColor)))
                            scope.launch {
                                snackbarHostState.showSnackbar(copiedMessage)
                            }
                        },
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (pendingDeleteId != null) {
        val title = stringResource(R.string.lienzo_color_delete_title)
        val message = stringResource(R.string.lienzo_color_delete_message)
        val confirm = stringResource(R.string.lienzo_color_delete_confirm)
        val cancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeColor(pendingDeleteId.orEmpty())
                        pendingDeleteId = null
                    },
                ) { Text(confirm) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text(cancel) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedColorCard(
    savedColor: SavedColor,
    isActionMenuVisible: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDismissAction: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
) {
    val rgb = ColorConverter.hexToRgb(savedColor.hex)
    val nameEntry = rgb?.let { ColorName.lookup(it) }
    val sourceFormat = runCatching { ColorFormat.valueOf(savedColor.sourceType) }.getOrDefault(ColorFormat.HEX)
    val sourceValue = savedColor.sourceValue ?: if (sourceFormat == ColorFormat.HEX) {
        savedColor.hex
    } else if (rgb != null) {
        formatForRgb(sourceFormat, rgb)
    } else {
        savedColor.hex
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeContainerBorderColor()),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        rgb?.let { Color(it.r, it.g, it.b) } ?: Color.Gray,
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = savedColor.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (nameEntry != null) {
                    Text(
                        text = "${nameEntry.chinese} · ${nameEntry.english}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = sourceValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                onClick = onCopy,
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = stringResource(R.string.lienzo_color_copied),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (isActionMenuVisible) {
            ColorCardActionMenu(
                onEdit = onEdit,
                onDelete = onDelete,
                onDismiss = onDismissAction,
            )
        }
    }
}

@Composable
private fun ColorCardActionMenu(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val popupOffsetY = with(LocalDensity.current) {
        -(8.dp + 40.dp).toPx().roundToInt()
    }
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(x = 0, y = popupOffsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorActionButton(
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                iconRes = R.drawable.ic_edit,
                contentDescription = stringResource(R.string.lienzo_color_edit),
                onClick = onEdit,
            )
            ColorActionButton(
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                iconRes = R.drawable.ic_delete,
                contentDescription = stringResource(R.string.lienzo_color_delete_title),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun ColorActionButton(
    color: Color,
    contentColor: Color,
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = color,
        contentColor = contentColor,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter = painterResource(iconRes), contentDescription = contentDescription)
        }
    }
}

private fun formatForRgb(format: ColorFormat, rgb: RgbColor): String {
    return when (format) {
        ColorFormat.HEX -> ColorConverter.rgbToHex(rgb)
        ColorFormat.RGB -> "RGB(${rgb.r}, ${rgb.g}, ${rgb.b})"
        ColorFormat.CMYK -> {
            val cmyk = ColorConverter.rgbToCmyk(rgb)
            "CMYK(${cmyk.c}%, ${cmyk.m}%, ${cmyk.y}%, ${cmyk.k}%)"
        }
        ColorFormat.HSL -> {
            val hsl = ColorConverter.rgbToHsl(rgb)
            "HSL(${hsl.h}, ${hsl.s}%, ${hsl.l}%)"
        }
        ColorFormat.HSV_HSB -> {
            val hsv = ColorConverter.rgbToHsv(rgb)
            "HSV(${hsv.h}, ${hsv.s}%, ${hsv.v}%)"
        }
        ColorFormat.NCS -> {
            val ncs = ColorConverter.rgbToNcs(rgb)
            "NCS S ${ncs.blackness.toString().padStart(2, '0')}${ncs.chromaticness.toString().padStart(2, '0')}-${ncs.hue}"
        }
        ColorFormat.LAB -> {
            val lab = ColorConverter.rgbToLab(rgb)
            "LAB(${lab.l}, ${lab.a}, ${lab.b})"
        }
    }
}
