package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.LogoSpec
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrMatrix
import com.hjinlabs.sengkode.core.model.QrStyle

/**
 * Converts (matrix, style, size) into a [DrawOp] list. Deterministic,
 * pure, and style-only: the ENCODED matrix is never modified for
 * visual effects - geometry, timing and alignment patterns survive
 * by construction because rendering reads the same bits the
 * round-trip decoder validates.
 */
class QrStyleRenderer {

    fun render(
        matrix: QrMatrix,
        style: QrStyle,
        sizePx: Int,
        logo: LogoImage? = null,
    ): List<DrawOp> {
        require(sizePx > 0) { "sizePx must be positive, was $sizePx" }
        val ops = mutableListOf<DrawOp>()

        // Canvas base: background everywhere (frame band included).
        ops += DrawOp.FillRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), style.backgroundArgb)

        val frame = style.frame
        val pad = if (frame != null) sizePx * FRAME_PADDING_FRACTION else 0f
        val labelBand = if (frame != null) sizePx * LABEL_BAND_FRACTION else 0f

        if (frame != null) {
            val stroke = sizePx * FRAME_STROKE_FRACTION
            val inset = stroke / 2f
            ops += DrawOp.StrokeRoundRect(
                x = inset,
                y = inset,
                w = sizePx - stroke,
                h = sizePx - stroke,
                radiusPx = sizePx * FRAME_RADIUS_FRACTION,
                strokePx = stroke,
                argb = style.foregroundArgb,
            )
            ops += DrawOp.Text(
                text = frame.labelText,
                centerX = sizePx / 2f,
                baselineY = sizePx - pad - labelBand * 0.32f,
                sizePx = labelBand * 0.60f,
                argb = style.foregroundArgb,
            )
        }

        val contentX = pad
        val contentY = pad
        val contentW = sizePx - 2 * pad
        val contentH = sizePx - 2 * pad - labelBand
        ops += DrawOp.FillRect(contentX, contentY, contentW, contentH, style.backgroundArgb)

        // Symbol layout: quiet zone + modules centered in content area.
        val total = matrix.width + 2 * style.quietZoneModules
        val cell = minOf(contentW, contentH) / total
        val originX = contentX + (contentW - cell * total) / 2f
        val originY = contentY + (contentH - cell * total) / 2f

        // Data modules (finder regions rendered separately below).
        val finders = finderRegions(matrix.width)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (inFinderRegion(x, y, finders)) continue
                if (matrix.isSet(x, y)) {
                    ops += moduleOp(
                        style,
                        originX + (x + style.quietZoneModules) * cell,
                        originY + (y + style.quietZoneModules) * cell,
                        cell,
                    )
                }
            }
        }

        // Finder patterns ("eyes"): outer 7x7 ring, 1-module gap
        // (background), inner 3x3 pupil - the exact structural ratios
        // a scanner locks onto. Shape and color vary; geometry never.
        for (finder in finders) {
            val ex = originX + (finder.x + style.quietZoneModules) * cell
            val ey = originY + (finder.y + style.quietZoneModules) * cell
            ops += eyeOps(style, ex, ey, cell)
        }

        // Center logo: drawn over the data modules only, never over
        // finders or quiet zone (guaranteed by the policy's
        // version-aware bounds; the renderer trusts only bounded specs).
        val logoSpec = style.logo
        if (logoSpec != null && logo != null) {
            val symbolSide = cell * matrix.width
            val logoSide = logoSpec.sizeFraction * symbolSide
            ops += DrawOp.Bitmap(
                x = originX + (style.quietZoneModules * cell) + (symbolSide - logoSide) / 2f,
                y = originY + (style.quietZoneModules * cell) + (symbolSide - logoSide) / 2f,
                w = logoSide,
                h = logoSide,
                pixels = logo.pixels,
                pixelWidth = logo.width,
                pixelHeight = logo.height,
            )
        }

        return ops
    }

    // -- internals -------------------------------------------------------

    private fun moduleOp(style: QrStyle, x: Float, y: Float, cell: Float): DrawOp = when (style.moduleShape) {
        ModuleShape.SQUARE -> DrawOp.FillRect(x, y, cell, cell, style.foregroundArgb)
        ModuleShape.ROUNDED -> DrawOp.RoundRect(
            x, y, cell, cell, radiusPx = cell * 0.28f, argb = style.foregroundArgb,
        )
        ModuleShape.DOT -> DrawOp.Circle(
            cx = x + cell / 2f, cy = y + cell / 2f, radius = cell * 0.42f, argb = style.foregroundArgb,
        )
    }

    private fun eyeOps(style: QrStyle, ex: Float, ey: Float, cell: Float): List<DrawOp> {
        val eyeColor = style.effectiveEyeColorArgb
        val backgroundColor = style.backgroundArgb
        val outer = 7 * cell
        val pupil = 3 * cell
        val ringOffset = cell
        val pupilOffset = 2 * cell

        val radiusOuter = when (style.eyeShape) {
            EyeShape.SQUARE -> 0f
            EyeShape.ROUNDED -> outer * 0.27f
        }
        val radiusPupil = when (style.eyeShape) {
            EyeShape.SQUARE -> 0f
            EyeShape.ROUNDED -> pupil * 0.27f
        }

        return listOf(
            // Outer 7x7 body
            if (radiusOuter > 0f) {
                DrawOp.RoundRect(ex, ey, outer, outer, radiusOuter, eyeColor)
            } else {
                DrawOp.FillRect(ex, ey, outer, outer, eyeColor)
            },
            // 1-module background separation (the ring's "hole")
            DrawOp.FillRect(ex + ringOffset, ey + ringOffset, 5 * cell, 5 * cell, backgroundColor),
            // Inner 3x3 pupil
            if (radiusPupil > 0f) {
                DrawOp.RoundRect(
                    ex + pupilOffset, ey + pupilOffset, pupil, pupil, radiusPupil, eyeColor,
                )
            } else {
                DrawOp.FillRect(ex + pupilOffset, ey + pupilOffset, pupil, pupil, eyeColor)
            },
        )
    }

    private data class Region(val x: Int, val y: Int)

    private fun finderRegions(width: Int): List<Region> = listOf(
        Region(0, 0),
        Region(width - 7, 0),
        Region(0, width - 7),
    )

    private fun inFinderRegion(x: Int, y: Int, finders: List<Region>): Boolean =
        finders.any { f ->
            x >= f.x && x < f.x + 7 && y >= f.y && y < f.y + 7
        }

    companion object {
        const val FRAME_PADDING_FRACTION = 0.06f
        const val LABEL_BAND_FRACTION = 0.085f
        const val FRAME_STROKE_FRACTION = 0.014f
        const val FRAME_RADIUS_FRACTION = 0.05f
        const val LOGO_SYMBOL_FRACTION = 1f // logo size is relative to symbol side
    }
}
