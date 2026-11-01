package com.mapherdf.maplenilset.ui.color

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapherdf.maplenilset.data.color.ColorConverter
import com.mapherdf.maplenilset.data.color.ColorName
import com.mapherdf.maplenilset.data.color.CmykColor
import com.mapherdf.maplenilset.data.color.HslColor
import com.mapherdf.maplenilset.data.color.HsvColor
import com.mapherdf.maplenilset.data.color.LabColor
import com.mapherdf.maplenilset.data.color.RgbColor
import com.mapherdf.maplenilset.data.color.SavedColor
import com.mapherdf.maplenilset.data.repository.ColorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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

    fun parseColor(format: ColorFormat, input: String): ColorParseResult? {
        val rgb = when (format) {
            ColorFormat.HEX -> ColorConverter.hexToRgb(input)
            ColorFormat.RGB -> parseRgb(input)
            ColorFormat.CMYK -> parseCmyk(input)?.let { ColorConverter.cmykToRgb(it) }
            ColorFormat.HSL -> parseHsl(input)?.let { ColorConverter.hslToRgb(it) }
            ColorFormat.HSV_HSB -> parseHsv(input)?.let { ColorConverter.hsvToRgb(it) }
            ColorFormat.NCS -> null
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
            "NCS S ${ncs.blackness}${ncs.chromaticness}-${ncs.hue}"
        }
        ColorFormat.LAB -> {
            val lab = ColorConverter.rgbToLab(rgb)
            "lab(${formatDouble(lab.l)}, ${formatDouble(lab.a)}, ${formatDouble(lab.b)})"
        }
    }

    fun colorName(rgb: RgbColor): ColorName.ColorNameEntry? = ColorName.lookup(rgb)

    fun saveColor(
        editId: String?,
        name: String,
        rgb: RgbColor,
        format: ColorFormat,
        inputValue: String,
        onSaved: () -> Unit,
    ) {
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
                createdAt = System.currentTimeMillis(),
            )
            if (editId != null) {
                colorRepository.updateColor(color)
            } else {
                colorRepository.addColor(color)
            }
            onSaved()
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

    private fun extractNumbers(input: String): List<Double> {
        return Regex("-?\\d+\\.?\\d*").findAll(input).mapNotNull { it.value.toDoubleOrNull() }.toList()
    }

    private fun formatDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
