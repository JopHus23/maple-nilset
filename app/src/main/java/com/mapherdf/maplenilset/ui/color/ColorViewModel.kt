package com.mapherdf.maplenilset.ui.color

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapherdf.maplenilset.data.color.ColorConverter
import com.mapherdf.maplenilset.data.color.ColorName
import com.mapherdf.maplenilset.data.color.ColorNameEntry
import com.mapherdf.maplenilset.data.color.CmykColor
import com.mapherdf.maplenilset.data.color.HslColor
import com.mapherdf.maplenilset.data.color.HsvColor
import com.mapherdf.maplenilset.data.color.LabColor
import com.mapherdf.maplenilset.data.color.NcsColor
import com.mapherdf.maplenilset.data.color.RgbColor
import com.mapherdf.maplenilset.data.color.SavedColor
import com.mapherdf.maplenilset.data.repository.ColorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class ColorFormat(val label: String) {
    HEX("HEX"),
    RGB("RGB"),
    CMYK("CMYK"),
    HSL("HSL"),
    HSV_HSB("HSV/HSB"),
    NCS("NCS"),
    LAB("LAB"),
}

data class ColorParseResult(
    val rgb: RgbColor,
    val hex: String,
)

@HiltViewModel
class ColorViewModel @Inject constructor(
    private val colorRepository: ColorRepository,
) : ViewModel() {

    val savedColors: StateFlow<List<SavedColor>> = colorRepository.savedColors
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _isSavingColor = MutableStateFlow(false)
    val isSavingColor: StateFlow<Boolean> = _isSavingColor

    fun parseColor(format: ColorFormat, input: String): ColorParseResult? {
        val rgb = when (format) {
            ColorFormat.HEX -> ColorConverter.hexToRgb(input)
            ColorFormat.RGB -> parseRgb(input)
            ColorFormat.CMYK -> parseCmyk(input)?.let { ColorConverter.cmykToRgb(it) }
            ColorFormat.HSL -> parseHsl(input)?.let { ColorConverter.hslToRgb(it) }
            ColorFormat.HSV_HSB -> parseHsv(input)?.let { ColorConverter.hsvToRgb(it) }
            ColorFormat.NCS -> parseNcs(input)?.let { ColorConverter.ncsToRgb(it) }
            ColorFormat.LAB -> parseLab(input)?.let { ColorConverter.labToRgb(it) }
        } ?: return null
        return ColorParseResult(rgb = rgb, hex = ColorConverter.rgbToHex(rgb))
    }

    fun formatForRgb(format: ColorFormat, rgb: RgbColor): String = when (format) {
        ColorFormat.HEX -> ColorConverter.rgbToHex(rgb)
        ColorFormat.RGB -> "rgb(${rgb.r}, ${rgb.g}, ${rgb.b})"
        ColorFormat.CMYK -> {
            val cmyk = ColorConverter.rgbToCmyk(rgb)
            "cmyk(${cmyk.c}%, ${cmyk.m}%, ${cmyk.y}%, ${cmyk.k}%)"
        }
        ColorFormat.HSL -> {
            val hsl = ColorConverter.rgbToHsl(rgb)
            "hsl(${hsl.h}, ${hsl.s}%, ${hsl.l}%)"
        }
        ColorFormat.HSV_HSB -> {
            val hsv = ColorConverter.rgbToHsv(rgb)
            "hsv(${hsv.h}, ${hsv.s}%, ${hsv.v}%)"
        }
        ColorFormat.NCS -> {
            val ncs = ColorConverter.rgbToNcs(rgb)
            "NCS S ${ncs.blackness.toString().padStart(2, '0')}${ncs.chromaticness.toString().padStart(2, '0')}-${ncs.hue}"
        }
        ColorFormat.LAB -> {
            val lab = ColorConverter.rgbToLab(rgb)
            "lab(${formatDouble(lab.l)}, ${formatDouble(lab.a)}, ${formatDouble(lab.b)})"
        }
    }

    fun colorName(rgb: RgbColor): ColorNameEntry? = ColorName.lookup(rgb)

    fun colorInfo(color: SavedColor): String {
        val rgb = ColorConverter.hexToRgb(color.hex) ?: return color.name
        val hsv = ColorConverter.rgbToHsv(rgb)
        val cmyk = ColorConverter.rgbToCmyk(rgb)
        val hsl = ColorConverter.rgbToHsl(rgb)
        val ncs = ColorConverter.rgbToNcs(rgb)
        val lab = ColorConverter.rgbToLab(rgb)
        val lch = ColorConverter.rgbToLch(rgb)
        val cie = ColorConverter.rgbToCieXy(rgb)
        val ncsCode = "${ncs.blackness.toString().padStart(2, '0')}${ncs.chromaticness.toString().padStart(2, '0')}-${ncs.hue}"
        return listOf(
            color.name,
            "HEX(${ColorConverter.rgbToHex(rgb)})",
            "RGB(${rgb.r}, ${rgb.g}, ${rgb.b})",
            "CMYK(${cmyk.c}%, ${cmyk.m}%, ${cmyk.y}%, ${cmyk.k}%)",
            "HSL(${hsl.h}, ${hsl.s}%, ${hsl.l}%)",
            "HSV(${hsv.h}, ${hsv.s}%, ${hsv.v}%)",
            "HSB(${hsv.h}, ${hsv.s}%, ${hsv.v}%)",
            "NCS S $ncsCode",
            "LAB(${formatDouble(lab.l)}, ${formatDouble(lab.a)}, ${formatDouble(lab.b)})",
            "LCh(${formatDouble(lch.l)}, ${formatDouble(lch.c)}, ${formatDouble(lch.h)})",
            "CIE-xy(${formatDouble(cie.x, 4)}, ${formatDouble(cie.y, 4)})",
        ).joinToString("\n")
    }

    fun saveColor(
        editId: String?,
        name: String,
        rgb: RgbColor,
        format: ColorFormat,
        inputValue: String,
        onSaved: () -> Unit,
    ) {
        if (_isSavingColor.value) return
        _isSavingColor.value = true
        viewModelScope.launch {
            val hex = ColorConverter.rgbToHex(rgb)
            val resolvedName = name.trim().ifEmpty {
                formatForRgb(format, rgb)
            }
            val color = SavedColor(
                id = editId ?: UUID.randomUUID().toString(),
                name = resolvedName,
                hex = hex,
                sourceType = format.name,
                sourceValue = inputValue,
                createdAt = System.currentTimeMillis(),
            )
            if (editId != null) {
                colorRepository.updateColor(color)
            } else {
                colorRepository.addColor(color)
            }
            onSaved()
            _isSavingColor.value = false
        }
    }

    fun removeColor(colorId: String) {
        viewModelScope.launch {
            colorRepository.removeColor(colorId)
        }
    }

    private fun parseRgb(input: String): RgbColor? {
        val numbers = extractNumbers(input)
        if (numbers.size < 3) return null
        return RgbColor(
            r = numbers[0].toInt().coerceIn(0, 255),
            g = numbers[1].toInt().coerceIn(0, 255),
            b = numbers[2].toInt().coerceIn(0, 255),
        )
    }

    private fun parseCmyk(input: String): CmykColor? {
        val numbers = extractNumbers(input)
        if (numbers.size < 4) return null
        return CmykColor(
            c = numbers[0].toInt().coerceIn(0, 100),
            m = numbers[1].toInt().coerceIn(0, 100),
            y = numbers[2].toInt().coerceIn(0, 100),
            k = numbers[3].toInt().coerceIn(0, 100),
        )
    }

    private fun parseHsl(input: String): HslColor? {
        val numbers = extractNumbers(input)
        if (numbers.size < 3) return null
        return HslColor(
            h = numbers[0].toInt().coerceIn(0, 360),
            s = numbers[1].toInt().coerceIn(0, 100),
            l = numbers[2].toInt().coerceIn(0, 100),
        )
    }

    private fun parseHsv(input: String): HsvColor? {
        val numbers = extractNumbers(input)
        if (numbers.size < 3) return null
        return HsvColor(
            h = numbers[0].toInt().coerceIn(0, 360),
            s = numbers[1].toInt().coerceIn(0, 100),
            v = numbers[2].toInt().coerceIn(0, 100),
        )
    }

    private fun parseLab(input: String): LabColor? {
        val numbers = extractNumbers(input)
        if (numbers.size < 3) return null
        return LabColor(
            l = numbers[0].coerceIn(0.0, 100.0),
            a = numbers[1].coerceIn(-128.0, 127.0),
            b = numbers[2].coerceIn(-128.0, 127.0),
        )
    }

    private fun parseNcs(input: String): NcsColor? {
        val normalized = input.uppercase().replace(Regex("\\s+"), " ").trim()
        val match = Regex("(?:S\\s*)?(\\d{2,4})[-\\s]?([YRGB]{2,4})").find(normalized) ?: return null
        val code = match.groupValues[1]
        val hue = match.groupValues[2]
        val blackness = code.substring(0, 2).toIntOrNull() ?: return null
        val chromaticness = code.substring(2).toIntOrNull() ?: return null
        return NcsColor(blackness, chromaticness, hue)
    }

    private fun extractNumbers(input: String): List<Double> {
        return Regex("-?\\d+\\.?\\d*").findAll(input).mapNotNull { it.value.toDoubleOrNull() }.toList()
    }

    private fun formatDouble(value: Double, digits: Int = 1): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.${digits}f", value)
        }
    }
}
