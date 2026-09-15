package com.hjinlabs.sengkode.core.style

/**
 * WCAG-style relative luminance and contrast ratio math. Used by the
 * scan-safety policy as a SUPPORTING HEURISTIC only - the final
 * scannability authority is always generate -> render -> decode.
 * Pure, deterministic, no platform dependencies.
 */
object ColorMath {

    /** WCAG 2.x relative luminance of an ARGB color (0.0..1.0). */
    fun relativeLuminance(argb: Long): Double {
        val r = linearize(((argb shr 16) and 0xFFL).toInt() / 255.0)
        val g = linearize(((argb shr 8) and 0xFFL).toInt() / 255.0)
        val b = linearize((argb and 0xFFL).toInt() / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** WCAG contrast ratio between two colors (1.0..21.0). */
    fun contrastRatio(a: Long, b: Long): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun linearize(channel: Double): Double =
        if (channel <= 0.04045) channel / 12.92
        else Math.pow((channel + 0.055) / 1.055, 2.4)
}
