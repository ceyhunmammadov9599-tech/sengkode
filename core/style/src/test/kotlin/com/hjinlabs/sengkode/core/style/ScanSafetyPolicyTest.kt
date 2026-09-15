package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.LogoSpec
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanSafetyPolicyTest {

    private val typicalWidth = 29 // version 3 symbol

    @Test
    fun `classic black on white is safe`() {
        val verdict = ScanSafetyPolicy.evaluate(QrStyle(), typicalWidth)
        assertTrue(verdict is ScanSafety.Safe)
    }

    @Test
    fun `inverted colors are unsafe with an explicit reason`() {
        val inverted = QrStyle(
            foregroundArgb = 0xFFFFFFFFL,
            backgroundArgb = 0xFF000000L,
        )
        val verdict = ScanSafetyPolicy.evaluate(inverted, typicalWidth)
        assertTrue(verdict is ScanSafety.Unsafe)
        assertTrue(
            (verdict as ScanSafety.Unsafe).reasons.any { it.contains("Inverted") },
        )
    }

    @Test
    fun `low contrast is unsafe with the measured ratio`() {
        val lowContrast = QrStyle(
            foregroundArgb = 0xFF888888L,
            backgroundArgb = 0xFFFFFFFFL,
        )
        val verdict = ScanSafetyPolicy.evaluate(lowContrast, typicalWidth)
        assertTrue(verdict is ScanSafety.Unsafe)
        assertTrue((verdict as ScanSafety.Unsafe).reasons.any { it.contains("4.5:1") })
    }

    @Test
    fun `dot shape demands higher contrast than square`() {
        // #757575 on white: ~4.6:1 - passes SQUARE, fails DOT's 5.5:1
        val style = QrStyle(
            foregroundArgb = 0xFF757575L,
            backgroundArgb = 0xFFFFFFFFL,
            moduleShape = ModuleShape.SQUARE,
        )
        assertTrue(ScanSafetyPolicy.evaluate(style, typicalWidth) is ScanSafety.Safe)
        val dots = style.copy(moduleShape = ModuleShape.DOT)
        val verdict = ScanSafetyPolicy.evaluate(dots, typicalWidth)
        assertTrue(verdict is ScanSafety.Unsafe)
    }

    @Test
    fun `quiet zone below spec minimum is unsafe`() {
        val style = QrStyle(quietZoneModules = 3)
        val verdict = ScanSafetyPolicy.evaluate(style, typicalWidth)
        assertTrue(verdict is ScanSafety.Unsafe)
        assertTrue(
            (verdict as ScanSafety.Unsafe).reasons.any { it.contains("quiet zone") },
        )
    }

    @Test
    fun `eye color must contrast the background`() {
        val style = QrStyle(eyeColorArgb = 0xFFB0BEC5L)
        val verdict = ScanSafetyPolicy.evaluate(style, typicalWidth)
        assertTrue(verdict is ScanSafety.Unsafe)
        assertTrue(
            (verdict as ScanSafety.Unsafe).reasons.any { it.contains("Eye") },
        )
    }

    @Test
    fun `logo above the global maximum is unsafe`() {
        val style = QrStyle(logo = LogoSpec(sizeFraction = 0.30f))
        val verdict = ScanSafetyPolicy.evaluate(style, typicalWidth)
        assertTrue(verdict is ScanSafety.Unsafe)
        assertTrue(
            (verdict as ScanSafety.Unsafe).reasons.any { it.contains("25% maximum") },
        )
    }

    @Test
    fun `logo fit is version aware - small symbols reject big logos`() {
        val bigLogo = LogoSpec(sizeFraction = 0.25f)
        // Version 1 symbol is 21 modules: max safe fraction is
        // (21 - 18) / 21 = 14.3% - a 25% logo would reach the finders.
        val smallSymbol = ScanSafetyPolicy.evaluate(
            QrStyle(logo = bigLogo), matrixWidth = 21,
        )
        assertTrue(smallSymbol is ScanSafety.Unsafe)
        assertTrue(
            (smallSymbol as ScanSafety.Unsafe).reasons.any { it.contains("finder") },
        )
        // The same logo on a version 3 symbol (29 modules) is fine:
        // (29 - 18) / 29 = 37.9%.
        val biggerSymbol = ScanSafetyPolicy.evaluate(
            QrStyle(logo = bigLogo), matrixWidth = 29,
        )
        assertTrue(biggerSymbol is ScanSafety.Safe)
    }

    @Test
    fun `logo adds an informative warning when safe`() {
        val verdict = ScanSafetyPolicy.evaluate(
            QrStyle(logo = LogoSpec(sizeFraction = 0.18f)),
            typicalWidth,
        )
        assertTrue(verdict is ScanSafety.Safe)
        assertTrue(
            (verdict as ScanSafety.Safe).warnings.any { it.contains("auto-upgraded") },
        )
    }

    @Test
    fun `verdicts are deterministic`() {
        val style = QrStyle(moduleShape = ModuleShape.DOT, logo = LogoSpec(0.2f))
        val first = ScanSafetyPolicy.evaluate(style, typicalWidth)
        repeat(5) {
            assertEquals(first, ScanSafetyPolicy.evaluate(style, typicalWidth))
        }
    }

    @Test
    fun `every shipped color preset is safe`() {
        StylePresets.COLOR_PRESETS.forEach { preset ->
            val style = QrStyle(
                foregroundArgb = preset.foregroundArgb,
                backgroundArgb = preset.backgroundArgb,
            )
            val verdict = ScanSafetyPolicy.evaluate(style, typicalWidth)
            assertTrue(
                "Preset ${preset.name} must be safe, was: $verdict",
                verdict is ScanSafety.Safe,
            )
        }
    }
}
