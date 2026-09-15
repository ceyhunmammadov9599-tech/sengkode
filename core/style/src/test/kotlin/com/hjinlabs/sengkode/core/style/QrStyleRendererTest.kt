package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.FrameSpec
import com.hjinlabs.sengkode.core.model.LogoSpec
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Geometry contract tests: styling may change APPEARANCE, never the
 * structural geometry a decoder depends on - finder patterns, quiet
 * zone, module coverage, logo bounds, frame separation.
 */
class QrStyleRendererTest {

    private val renderer = QrStyleRenderer()
    private val engine = ZxingQrEngine()
    private val size = 512

    private fun matrix(): com.hjinlabs.sengkode.core.model.QrMatrix =
        ((engine.generate(QrContent.Text("SENGKODE geometry"), EccLevelDefault)
            as QrGenerationResult.Success)).matrix

    private val EccLevelDefault = com.hjinlabs.sengkode.core.model.EccLevel.M

    // -- finder pattern preservation ----------------------------------

    @Test
    fun `every finder region gets exactly the three-eye structure`() {
        val ops = renderer.render(matrix(), QrStyle(), size)
        val w = matrix().width
        val total = w + 8 // default quiet zone 4 * 2
        val cell = (size.toFloat()) / total

        // Three eyes -> exactly 9 eye ops (3 per eye), each sized for
        // 7, 5 and 3 modules respectively.
        val outerOps = ops.filter { it is DrawOp.FillRect && it.w == 7 * cell && it.h == 7 * cell }
        val innerOps = ops.filter { it is DrawOp.FillRect && it.w == 3 * cell && it.h == 3 * cell }
        assertEquals(3, outerOps.size)
        assertEquals(3, innerOps.size)
    }

    @Test
    fun `no data module is rendered inside finder regions`() {
        val style = QrStyle()
        val m = matrix()
        val ops = renderer.render(m, style, size)
        val total = m.width + 8
        val cell = size.toFloat() / total
        val finders = listOf(
            0 to 0,
            m.width - 7 to 0,
            0 to m.width - 7,
        )
        val dataOps = ops.filterIsInstance<DrawOp.FillRect>()
            .filter { it.w == cell } // module-sized ops only
        dataOps.forEach { op ->
            val mx = ((op.x / cell) - 4).toInt()
            val my = ((op.y / cell) - 4).toInt()
            finders.forEach { (fx, fy) ->
                assertFalse(
                    "module at ($mx,$my) overlaps finder at ($fx,$fy)",
                    mx >= fx && mx < fx + 7 && my >= fy && my < fy + 7,
                )
            }
        }
    }

    @Test
    fun `rounded eyes keep the structural 7-5-3 layering`() {
        val ops = renderer.render(
            matrix(),
            QrStyle(eyeShape = EyeShape.ROUNDED),
            size,
        )
        val rounded = ops.filterIsInstance<DrawOp.RoundRect>()
        // 3 eyes x (outer + pupil) = 6 rounded ops
        assertEquals(6, rounded.size)
    }

    // -- quiet zone + frame separation --------------------------------

    @Test
    fun `frames never touch the symbol or quiet zone`() {
        val style = QrStyle(frame = FrameSpec())
        val m = matrix()
        val ops = renderer.render(m, style, size)

        val pad = size * QrStyleRenderer.FRAME_PADDING_FRACTION
        val labelBand = size * QrStyleRenderer.LABEL_BAND_FRACTION

        // Every module/eye/logo op must sit strictly inside the frame band.
        ops.filter { it !is DrawOp.Text && it !is DrawOp.StrokeRoundRect }
            .filter { it !is DrawOp.FillRect || it.w < size } // skip full-canvas fills
            .forEach { op ->
                val (x, y) = when (op) {
                    is DrawOp.FillRect -> op.x to op.y
                    is DrawOp.RoundRect -> op.x to op.y
                    is DrawOp.Circle -> op.cx - op.radius to op.cy - op.radius
                    is DrawOp.Bitmap -> op.x to op.y
                    is DrawOp.StrokeRoundRect -> op.x to op.y
                    is DrawOp.Text -> 0f to 0f
                }
                assertTrue("op intrudes the frame band: $op", x >= pad - 0.5f)
                assertTrue("op intrudes the frame band: $op", y >= pad - 0.5f)
            }
        // The label op exists exactly once and lives in the bottom band.
        val text = ops.filterIsInstance<DrawOp.Text>()
        assertEquals(1, text.size)
        assertTrue(text[0].baselineY > size - pad - labelBand)
    }

    @Test
    fun `logo op is centered and bounded to the symbol`() {
        val m = matrix()
        val fraction = 0.20f
        val style = QrStyle(logo = LogoSpec(sizeFraction = fraction))
        val logo = LogoImage(
            pixels = IntArray(16 * 16) { 0xFF000000.toInt() },
            width = 16, height = 16,
        )
        val ops = renderer.render(m, style, size, logo)
        val bitmaps = ops.filterIsInstance<DrawOp.Bitmap>()
        assertEquals(1, bitmaps.size)
        val op = bitmaps[0]
        val total = m.width + 8
        val cell = size.toFloat() / total
        val symbolSide = cell * m.width
        val expectedLogoSide = fraction * symbolSide
        // Centered inside the symbol, not the canvas:
        val symbolX0 = 4 * cell
        val centerX = symbolX0 + (symbolSide - op.w) / 2f
        assertEquals(centerX, op.x, 0.5f)
        assertEquals(expectedLogoSide, op.w, 0.5f)
        // Logo never reaches finders (7 modules each side):
        assertTrue(op.x > symbolX0 + 7 * cell)
        assertTrue(op.x + op.w < symbolX0 + symbolSide - 7 * cell)
    }

    @Test
    fun `dot modules stay within their cell`() {
        val m = matrix()
        val ops = renderer.render(m, QrStyle(moduleShape = ModuleShape.DOT), size)
        val total = m.width + 8
        val cell = size.toFloat() / total
        ops.filterIsInstance<DrawOp.Circle>().forEach { c ->
            assertTrue(c.radius <= cell / 2f + 0.01f)
        }
        // A version-2+ symbol always has data modules somewhere.
        assertTrue(ops.filterIsInstance<DrawOp.Circle>().isNotEmpty())
    }

    @Test
    fun `rendering is deterministic`() {
        val m = matrix()
        val style = QrStyle(moduleShape = ModuleShape.ROUNDED, eyeShape = EyeShape.ROUNDED)
        val first = renderer.render(m, style, size)
        repeat(3) {
            assertEquals(first, renderer.render(m, style, size))
        }
    }
}
