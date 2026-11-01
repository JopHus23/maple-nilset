package com.mapherdf.maplenilset.data.color

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class RgbColor(val r: Int, val g: Int, val b: Int)

data class HslColor(val h: Int, val s: Int, val l: Int)

data class HsvColor(val h: Int, val s: Int, val v: Int)

data class CmykColor(val c: Int, val m: Int, val y: Int, val k: Int)

data class LabColor(val l: Double, val a: Double, val b: Double)

data class LchColor(val l: Double, val c: Double, val h: Double)

data class CieXyColor(val x: Double, val y: Double)

data class NcsColor(val blackness: Int, val chromaticness: Int, val hue: String)

object ColorConverter {

    fun hexToRgb(hex: String): RgbColor? {
        val clean = hex.removePrefix("#").trim()
        if (clean.length != 6) return null
        val value = clean.toIntOrNull(16) ?: return null
        return RgbColor(
            r = (value shr 16) and 0xFF,
            g = (value shr 8) and 0xFF,
            b = value and 0xFF,
        )
    }

    fun rgbToHex(rgb: RgbColor): String {
        return String.format("#%02X%02X%02X", rgb.r, rgb.g, rgb.b)
    }

    fun rgbToHsl(rgb: RgbColor): HslColor {
        val rf = rgb.r / 255.0
        val gf = rgb.g / 255.0
        val bf = rgb.b / 255.0
        val maxValue = max(rf, max(gf, bf))
        val minValue = min(rf, min(gf, bf))
        val delta = maxValue - minValue
        val l = (maxValue + minValue) / 2.0
        if (delta == 0.0) return HslColor(0, 0, (l * 100).roundToInt())
        val s = delta / (1 - abs(2 * l - 1))
        val h = when (maxValue) {
            rf -> ((gf - bf) / delta + if (gf < bf) 6.0 else 0.0) * 60
            gf -> ((bf - rf) / delta + 2.0) * 60
            else -> ((rf - gf) / delta + 4.0) * 60
        }
        return HslColor(h.roundToInt(), (s * 100).roundToInt(), (l * 100).roundToInt())
    }

    fun rgbToHsv(rgb: RgbColor): HsvColor {
        val rf = rgb.r / 255.0
        val gf = rgb.g / 255.0
        val bf = rgb.b / 255.0
        val maxValue = max(rf, max(gf, bf)).coerceAtLeast(0.0)
        val minValue = min(rf, min(gf, bf))
        val delta = maxValue - minValue
        val v = maxValue
        if (delta == 0.0) return HsvColor(0, 0, (v * 100).roundToInt())
        val s = delta / maxValue
        val h = when (maxValue) {
            rf -> ((gf - bf) / delta + if (gf < bf) 6.0 else 0.0) * 60
            gf -> ((bf - rf) / delta + 2.0) * 60
            else -> ((rf - gf) / delta + 4.0) * 60
        }
        return HsvColor(h.roundToInt(), (s * 100).roundToInt(), (v * 100).roundToInt())
    }

    fun rgbToCmyk(rgb: RgbColor): CmykColor {
        val rf = rgb.r / 255.0
        val gf = rgb.g / 255.0
        val bf = rgb.b / 255.0
        val k = 1 - max(rf, max(gf, bf))
        if (k >= 1.0) return CmykColor(0, 0, 0, 100)
        val c = (1 - rf - k) / (1 - k)
        val m = (1 - gf - k) / (1 - k)
        val y = (1 - bf - k) / (1 - k)
        return CmykColor(
            (c * 100).roundToInt(),
            (m * 100).roundToInt(),
            (y * 100).roundToInt(),
            (k * 100).roundToInt(),
        )
    }

    fun rgbToLab(rgb: RgbColor): LabColor {
        val rf = pivotXyz(rgb.r / 255.0)
        val gf = pivotXyz(rgb.g / 255.0)
        val bf = pivotXyz(rgb.b / 255.0)
        val x = rf * 0.4124 + gf * 0.3576 + bf * 0.1805
        val y = rf * 0.2126 + gf * 0.7152 + bf * 0.0722
        val z = rf * 0.0193 + gf * 0.1192 + bf * 0.9505
        val refX = 0.95047
        val refY = 1.0
        val refZ = 1.08883
        val fx = pivotLab(x / refX)
        val fy = pivotLab(y / refY)
        val fz = pivotLab(z / refZ)
        return LabColor(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    fun labToRgb(lab: LabColor): RgbColor? {
        val fy = (lab.l + 16) / 116.0
        val fx = fy + lab.a / 500.0
        val fz = fy - lab.b / 200.0
        val refX = 0.95047
        val refY = 1.0
        val refZ = 1.08883
        val x = pivotLabInverse(fx) * refX
        val y = pivotLabInverse(fy) * refY
        val z = pivotLabInverse(fz) * refZ
        val rf = x * 3.2406 + y * -1.5372 + z * -0.4986
        val gf = x * -0.9689 + y * 1.8758 + z * 0.0415
        val bf = x * 0.0557 + y * -0.2040 + z * 1.0570
        val r = (pivotXyzInverse(rf) * 255).roundToInt().coerceIn(0, 255)
        val g = (pivotXyzInverse(gf) * 255).roundToInt().coerceIn(0, 255)
        val b = (pivotXyzInverse(bf) * 255).roundToInt().coerceIn(0, 255)
        return RgbColor(r, g, b)
    }

    fun rgbToLch(rgb: RgbColor): LchColor {
        val lab = rgbToLab(rgb)
        val c = sqrt(lab.a * lab.a + lab.b * lab.b)
        var h = Math.toDegrees(kotlin.math.atan2(lab.b, lab.a))
        if (h < 0) h += 360
        return LchColor(lab.l, c, h)
    }

    fun rgbToCieXy(rgb: RgbColor): CieXyColor {
        val rf = srgbToLinear(rgb.r / 255.0)
        val gf = srgbToLinear(rgb.g / 255.0)
        val bf = srgbToLinear(rgb.b / 255.0)
        val x = rf * 0.4124 + gf * 0.3576 + bf * 0.1805
        val y = rf * 0.2126 + gf * 0.7152 + bf * 0.0722
        val z = rf * 0.0193 + gf * 0.1192 + bf * 0.9505
        val sum = x + y + z
        if (sum == 0.0) return CieXyColor(0.3333, 0.3333)
        return CieXyColor(x / sum, y / sum)
    }

    fun rgbToNcs(rgb: RgbColor): NcsColor {
        val lab = rgbToLab(rgb)
        val c = sqrt(lab.a * lab.a + lab.b * lab.b)
        val chromaticness = (c / 1.0 * 100).roundToInt().coerceIn(0, 100)
        val blackness = ((100 - lab.l) * 0.9).roundToInt().coerceIn(0, 100)
        var hueAngle = Math.toDegrees(kotlin.math.atan2(lab.b, lab.a))
        if (hueAngle < 0) hueAngle += 360
        val hue = ncsHueName(hueAngle)
        return NcsColor(blackness, chromaticness, hue)
    }

    fun hslToRgb(hsl: HslColor): RgbColor {
        val h = hsl.h / 360.0
        val s = hsl.s / 100.0
        val l = hsl.l / 100.0
        if (s == 0.0) {
            val gray = (l * 255).roundToInt()
            return RgbColor(gray, gray, gray)
        }
        val q = if (l < 0.5) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        return RgbColor(
            r = hueToRgb(p, q, h + 1.0 / 3.0),
            g = hueToRgb(p, q, h),
            b = hueToRgb(p, q, h - 1.0 / 3.0),
        )
    }

    fun hsvToRgb(hsv: HsvColor): RgbColor {
        val h = hsv.h / 60.0
        val s = hsv.s / 100.0
        val v = hsv.v / 100.0
        val i = h.toInt() % 6
        val f = h - h.toInt()
        val p = v * (1 - s)
        val q = v * (1 - f * s)
        val t = v * (1 - (1 - f) * s)
        return when (i) {
            0 -> RgbColor((v * 255).roundToInt(), (t * 255).roundToInt(), (p * 255).roundToInt())
            1 -> RgbColor((q * 255).roundToInt(), (v * 255).roundToInt(), (p * 255).roundToInt())
            2 -> RgbColor((p * 255).roundToInt(), (v * 255).roundToInt(), (t * 255).roundToInt())
            3 -> RgbColor((p * 255).roundToInt(), (q * 255).roundToInt(), (v * 255).roundToInt())
            4 -> RgbColor((t * 255).roundToInt(), (p * 255).roundToInt(), (v * 255).roundToInt())
            else -> RgbColor((v * 255).roundToInt(), (p * 255).roundToInt(), (q * 255).roundToInt())
        }
    }

    fun cmykToRgb(cmyk: CmykColor): RgbColor {
        val c = cmyk.c / 100.0
        val m = cmyk.m / 100.0
        val y = cmyk.y / 100.0
        val k = cmyk.k / 100.0
        return RgbColor(
            r = (255 * (1 - min(1.0, c * (1 - k) + k))).roundToInt(),
            g = (255 * (1 - min(1.0, m * (1 - k) + k))).roundToInt(),
            b = (255 * (1 - min(1.0, y * (1 - k) + k))).roundToInt(),
        )
    }

    fun relativeLuminance(rgb: RgbColor): Double {
        val rf = srgbToLinear(rgb.r / 255.0)
        val gf = srgbToLinear(rgb.g / 255.0)
        val bf = srgbToLinear(rgb.b / 255.0)
        return 0.2126 * rf + 0.7152 * gf + 0.0722 * bf
    }

    private fun hueToRgb(p: Double, q: Double, t: Double): Int {
        var hue = t
        if (hue < 0) hue += 1
        if (hue > 1) hue -= 1
        val value = when {
            hue < 1.0 / 6.0 -> p + (q - p) * 6 * hue
            hue < 1.0 / 2.0 -> q
            hue < 2.0 / 3.0 -> p + (q - p) * (2.0 / 3.0 - hue) * 6
            else -> p
        }
        return (value * 255).roundToInt()
    }

    private fun srgbToLinear(channel: Double): Double {
        return if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
    }

    private fun pivotXyz(channel: Double): Double {
        return if (channel > 0.04045) ((channel + 0.055) / 1.055).pow(2.4) else channel / 12.92
    }

    private fun pivotXyzInverse(channel: Double): Double {
        return if (channel > 0.0031308) 1.055 * channel.pow(1.0 / 2.4) - 0.055 else channel * 12.92
    }

    private fun pivotLab(t: Double): Double {
        return if (t > 0.008856451679035631) cbrt(t) else t / (3 * 0.008856451679035631) + 4.0 / 29
    }

    private fun pivotLabInverse(t: Double): Double {
        return if (t > 6.0 / 29) t * t * t else 3 * 0.008856451679035631 * (t - 4.0 / 29)
    }

    private fun ncsHueName(angle: Double): String {
        val sectors = listOf(
            0.0 to "Y", 30.0 to "YG", 60.0 to "G", 90.0 to "GB",
            120.0 to "B", 150.0 to "RB", 180.0 to "R",
        )
        val shifted = if (angle >= 0 && angle <= 180) angle else if (angle > 180) angle - 360 else angle + 360
        for (i in 0 until sectors.size - 1) {
            if (shifted >= sectors[i].first && shifted < sectors[i + 1].first) {
                val ratio = (shifted - sectors[i].first) / 30.0
                val pct = (ratio * 100).roundToInt()
                return if (pct == 0) sectors[i].second else if (pct == 100) sectors[i + 1].second else {
                    "${sectors[i + 1].second}${pct}${sectors[i].second}"
                }
            }
        }
        return "Y"
    }
}
