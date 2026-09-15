package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.LogoSpec
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrStyle
import kotlin.math.roundToInt

/**
 * Deterministic verdict of whether a style is safe to render.
 * Centralized by design - no color checks live in UI composables.
 *
 * The WCAG-derived contrast thresholds are a heuristic gate: they
 * catch obviously unsafe combinations BEFORE wasting a generation,
 * with reasons the UI shows verbatim. The actual authority remains
 * the round-trip suite in this module's tests.
 */
sealed interface ScanSafety {
    data class Safe(val warnings: List<String> = emptyList()) : ScanSafety
    data class Unsafe(val reasons: List<String>) : ScanSafety

    val isSafe: Boolean
        get() = this is Safe
}

object ScanSafetyPolicy {

    /** WCAG AA-derived minimum for module/background contrast. */
    const val MIN_MODULE_CONTRAST = 4.5

    /** Extra margin for DOT modules: less area -> decoder needs more contrast. */
    const val MIN_DOT_CONTRAST = 5.5

    /**
     * Modules at [matrixWidth] modules wide. The logo rule is
     * version-aware: the central logo must keep at least a
     * 2-module gap from every finder pattern.
     */
    fun evaluate(style: QrStyle, matrixWidth: Int): ScanSafety {
        val reasons = mutableListOf<String>()
        val fg = style.foregroundArgb
        val bg = style.backgroundArgb

        val fgLum = ColorMath.relativeLuminance(fg)
        val bgLum = ColorMath.relativeLuminance(bg)
        val ratio = ColorMath.contrastRatio(fg, bg)

        // Inverted codes (light modules on dark background) fail most
        // real scanners even at high contrast.
        if (fgLum >= bgLum) {
            reasons += "Dark modules must be darker than the background. " +
                "Inverted codes fail most scanners."
        }

        val minContrast = when (style.moduleShape) {
            ModuleShape.DOT -> MIN_DOT_CONTRAST
            else -> MIN_MODULE_CONTRAST
        }
        if (ratio < minContrast) {
            reasons += "Module/background contrast is ${"%.1f".format(ratio)}:1, " +
                "below the ${"%.1f".format(minContrast)}:1 minimum" +
                (if (style.moduleShape == ModuleShape.DOT) " for dot shapes" else "") +
                "."
        }

        if (style.quietZoneModules < 4) {
            reasons += "The quiet zone must be at least 4 modules wide " +
                "(QR specification minimum)."
        }

        val eye = style.effectiveEyeColorArgb
        if (eye != fg) {
            val eyeRatio = ColorMath.contrastRatio(eye, bg)
            if (ColorMath.relativeLuminance(eye) >= bgLum) {
                reasons += "Eye color must be darker than the background."
            }
            if (eyeRatio < MIN_MODULE_CONTRAST) {
                reasons += "Eye/background contrast is ${"%.1f".format(eyeRatio)}:1, " +
                    "below the ${MIN_MODULE_CONTRAST}:1 minimum."
            }
        }

        style.logo?.let { logo ->
            if (logo.sizeFraction > LogoSpec.MAX_FRACTION) {
                reasons += "Logo size is ${(logo.sizeFraction * 100).roundToInt()}% of the " +
                    "symbol, above the ${((LogoSpec.MAX_FRACTION * 100).roundToInt())}% maximum."
            }
            if (logo.sizeFraction < LogoSpec.MIN_FRACTION) {
                reasons += "Logo size is below the " +
                    "${((LogoSpec.MIN_FRACTION * 100).roundToInt())}% minimum."
            }
            // Version-aware fit: on EACH side of the centered logo the
            // 7-module finder plus a 2-module safety gap must still fit:
            // logoSide <= matrixWidth - 2 * (7 + 2).
            val maxFractionForVersion =
                (matrixWidth - 2 * (FINDER_MODULES + LOGO_FINDER_GAP)) /
                    matrixWidth.toFloat()
            if (logo.sizeFraction > maxFractionForVersion) {
                reasons += "The logo is too large for this QR size " +
                    "(${matrixWidth}x${matrixWidth} modules) - it would reach " +
                    "the finder patterns. Shorten the content (bigger symbol) " +
                    "or shrink the logo."
            }
        }

        return if (reasons.isEmpty()) ScanSafety.Safe(
            warnings = buildWarnings(style),
        ) else ScanSafety.Unsafe(reasons)
    }

    private fun buildWarnings(style: QrStyle): List<String> = buildList {
        if (style.logo != null) {
            add("Error correction is auto-upgraded for the logo.")
        }
        if (style.moduleShape == ModuleShape.DOT) {
            add("Dot shapes need a bright, glare-free scan surface.")
        }
        if (style.eyeShape == EyeShape.ROUNDED) {
            add("Rounded eyes are safe; scan from a moderate distance.")
        }
    }

    private const val FINDER_MODULES = 7
    private const val LOGO_FINDER_GAP = 2
}
