package com.mapherdf.maplenilset.ui.color

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapherdf.maplenilset.R
import com.mapherdf.maplenilset.data.color.ColorConverter
import com.mapherdf.maplenilset.data.color.ColorName
import com.mapherdf.maplenilset.data.color.RgbColor
import com.mapherdf.maplenilset.data.color.SavedColor
import com.mapherdf.maplenilset.ui.theme.themeContainerBorderColor
import com.mapherdf.maplenilset.ui.theme.themeContainerColor

private val CieSpectralLocus = listOf(
    0.1741 to 0.0050,
    0.1733 to 0.0038,
    0.1596 to 0.0044,
    0.1443 to 0.0071,
    0.1320 to 0.0102,
    0.1272 to 0.0124,
    0.1440 to 0.0292,
    0.1241 to 0.0711,
    0.0913 to 0.1327,
    0.0454 to 0.2950,
    0.0082 to 0.5384,
    0.0131 to 0.6653,
    0.0743 to 0.8338,
    0.1547 to 0.8059,
    0.2143 to 0.7502,
    0.3016 to 0.6923,
    0.3731 to 0.6245,
    0.4441 to 0.5547,
    0.5125 to 0.4866,
    0.5752 to 0.4207,
    0.6270 to 0.3725,
    0.6658 to 0.3340,
    0.6915 to 0.3083,
    0.7079 to 0.2920,
    0.7190 to 0.2811,
    0.7347 to 0.2653,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDetailScreen(
    colorId: String,
    onNavigateBack: () -> Unit,
    onEditColor: (String) -> Unit,
    viewModel: ColorViewModel = hiltViewModel(),
) {
    val savedColors by viewModel.savedColors.collectAsStateWithLifecycle()
    val savedColor = savedColors.find { it.id == colorId }
    val clipboard = LocalClipboardManager.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lienzo_color_detail)) },
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
                actions = {
                    IconButton(
                        onClick = { onEditColor(colorId) },
                        enabled = savedColor != null,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = stringResource(R.string.lienzo_color_edit),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = savedColor != null,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.lienzo_color_delete_title),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (savedColor == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.lienzo_color_empty))
            }
        } else {
            val rgb = ColorConverter.hexToRgb(savedColor.hex) ?: RgbColor(128, 128, 128)
            val colorName = ColorName.lookup(rgb)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ColorHeader(savedColor, rgb, colorName)
                ColorFormatCards(rgb) { value ->
                    clipboard.setText(AnnotatedString(value))
                }
                CiePositionCard(rgb)
                PerceptualPropertiesCard(rgb)
            }
        }
    }

    if (showDeleteDialog) {
        val title = stringResource(R.string.lienzo_color_delete_title)
        val message = stringResource(R.string.lienzo_color_delete_message)
        val confirm = stringResource(R.string.lienzo_color_delete_confirm)
        val cancel = stringResource(R.string.cancel)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.removeColor(colorId)
                        onNavigateBack()
                    },
                ) { Text(confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(cancel) }
            },
        )
    }
}

@Composable
private fun ColorHeader(
    savedColor: SavedColor,
    rgb: RgbColor,
    colorName: com.mapherdf.maplenilset.data.color.ColorNameEntry?,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(Color(rgb.r, rgb.g, rgb.b)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = savedColor.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (colorName != null) {
                    Text(
                        text = "${colorName.chinese} · ${colorName.english}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = savedColor.hex,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ColorFormatCards(
    rgb: RgbColor,
    onCopyValue: (String) -> Unit,
) {
    val formats = listOf(
        ColorFormat.HEX,
        ColorFormat.RGB,
        ColorFormat.CMYK,
        ColorFormat.HSL,
        ColorFormat.HSV_HSB,
        ColorFormat.NCS,
        ColorFormat.LAB,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        formats.forEach { format ->
            InfoCard(
                title = format.label,
                description = formatDescription(format),
                value = formatValue(format, rgb),
                onCopy = { onCopyValue(formatValue(format, rgb)) },
            )
        }
        val lch = ColorConverter.rgbToLch(rgb)
        InfoCard(
            title = "LCh",
            description = "CIE LCh：由 LAB 的 a*、b* 换算出的彩度与色相角。",
            value = "LCh(${formatValue(lch.l)}, ${formatValue(lch.c)}, ${formatValue(lch.h)})",
            onCopy = { onCopyValue("LCh(${formatValue(lch.l)}, ${formatValue(lch.c)}, ${formatValue(lch.h)})") },
        )
        val cie = ColorConverter.rgbToCieXy(rgb)
        InfoCard(
            title = "CIE-xy",
            description = "CIE 1931 色度坐标，表示颜色在标准色度图中的位置。",
            value = "CIE-xy(${formatValue(cie.x, 4)}, ${formatValue(cie.y, 4)})",
            onCopy = { onCopyValue("CIE-xy(${formatValue(cie.x, 4)}, ${formatValue(cie.y, 4)})") },
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    description: String,
    value: String,
    onCopy: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    onClick = onCopy,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_copy),
                            contentDescription = stringResource(R.string.lienzo_color_copied),
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CiePositionCard(rgb: RgbColor) {
    val cie = ColorConverter.rgbToCieXy(rgb)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.lienzo_color_domain_position),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                fun x(value: Double): Float = (value / 0.80 * size.width).toFloat()
                fun y(value: Double): Float = size.height * (1f - (value / 0.90).toFloat())
                val red = Offset(x(0.640), y(0.330))
                val green = Offset(x(0.300), y(0.600))
                val blue = Offset(x(0.150), y(0.060))
                val triangle = Path().apply {
                    moveTo(red.x, red.y)
                    lineTo(green.x, green.y)
                    lineTo(blue.x, blue.y)
                    close()
                }
                val spectralLocus = Path().apply {
                    CieSpectralLocus.forEachIndexed { index, (xValue, yValue) ->
                        val point = Offset(x(xValue), y(yValue))
                        if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    close()
                }
                drawPath(spectralLocus, Color.White.copy(alpha = 0.08f))
                drawPath(spectralLocus, Color.White.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx()))
                drawPath(triangle, Color.White.copy(alpha = 0.10f))
                drawPath(triangle, Color.White.copy(alpha = 0.35f), style = Stroke(width = 2.dp.toPx()))
                val point = Offset(x(cie.x), y(cie.y))
                drawCircle(Color(rgb.r, rgb.g, rgb.b), radius = 10.dp.toPx(), center = point)
                drawCircle(Color.White, radius = 10.dp.toPx(), center = point, style = Stroke(width = 2.dp.toPx()))
            }
            val gamutStatus = stringResource(
                if (ColorConverter.isInsideSrgbGamut(cie)) {
                    R.string.lienzo_color_srgb_inside
                } else {
                    R.string.lienzo_color_srgb_outside
                },
            )
            Text(
                text = "CIE-xy(${formatValue(cie.x, 4)}, ${formatValue(cie.y, 4)}) · $gamutStatus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PerceptualPropertiesCard(rgb: RgbColor) {
    val lab = ColorConverter.rgbToLab(rgb)
    val lch = ColorConverter.rgbToLch(rgb)
    val hsv = ColorConverter.rgbToHsv(rgb)
    val hsl = ColorConverter.rgbToHsl(rgb)
    val luminance = ColorConverter.relativeLuminance(rgb)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = themeContainerColor(),
        border = BorderStroke(1.dp, themeContainerBorderColor()),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.lienzo_color_perceptual),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            PerceptualBar(stringResource(R.string.lienzo_color_lightness), lab.l / 100.0, formatValue(lab.l))
            PerceptualBar(stringResource(R.string.lienzo_color_chroma), (lch.c / 133.0).coerceIn(0.0, 1.0), formatValue(lch.c))
            PerceptualBar(stringResource(R.string.lienzo_color_brightness), hsv.v / 100.0, "${hsv.v}%")
            PerceptualBar(stringResource(R.string.lienzo_color_saturation_hsl), hsl.s / 100.0, "${hsl.s}%")
            PerceptualBar(stringResource(R.string.lienzo_color_relative_luminance), luminance, formatValue(luminance, 4))
        }
    }
}

@Composable
private fun PerceptualBar(
    label: String,
    value: Double,
    displayValue: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { value.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
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

private fun formatValue(format: ColorFormat, rgb: RgbColor): String {
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
            "LAB(${formatValue(lab.l)}, ${formatValue(lab.a)}, ${formatValue(lab.b)})"
        }
    }
}

private fun formatValue(value: Double, digits: Int = 1): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.${digits}f", value)
    }
}
