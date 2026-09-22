package com.hjinlabs.sengkode.core.export

import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * DrawListBitmapRenderer contract tests.
 *
 * The renderer is the single rendering path for preview, export, and
 * batch. These tests verify:
 * - Output bitmap dimensions match the requested size.
 * - Square, dot, and rounded module shapes all produce non-blank output.
 * - Rendering is deterministic (same inputs -> identical pixel output).
 * - A rendered bitmap passes the scannability verifier (round-trip).
 *
 * Robolectric is required because the renderer calls android.graphics.Canvas.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DrawListBitmapRendererTest {

    private val engine = ZxingQrEngine()
    private val renderer = DrawListBitmapRenderer()

    private fun matrix(text: String = "SENGKODE renderer test") =
        (engine.generate(QrContent.Text(text), EccLevel.M) as QrGenerationResult.Success).matrix

    // ---- Output dimensions ------------------------------------------

    @Test
    fun `output bitmap is exactly the requested square size`() {
        val sizes = listOf(128, 256, 512, 1024)
        val m = matrix()
        sizes.forEach { size ->
            val bitmap = renderer.render(m, QrStyle(), size)
            assertEquals("width at size $size", size, bitmap.width)
            assertEquals("height at size $size", size, bitmap.height)
        }
    }

    // ---- Module shapes produce non-blank output ---------------------

    @Test
    fun `square module style renders a non-blank bitmap`() {
        val bitmap = renderer.render(matrix(), QrStyle(moduleShape = ModuleShape.SQUARE), 256)
        assertTrue(hasNonWhitePixels(bitmap))
    }

    @Test
    fun `dot module style renders a non-blank bitmap`() {
        val bitmap = renderer.render(matrix(), QrStyle(moduleShape = ModuleShape.DOT), 256)
        assertTrue(hasNonWhitePixels(bitmap))
    }

    @Test
    fun `rounded module style renders a non-blank bitmap`() {
        val bitmap = renderer.render(matrix(), QrStyle(moduleShape = ModuleShape.ROUNDED), 256)
        assertTrue(hasNonWhitePixels(bitmap))
    }

    @Test
    fun `rounded eye shape renders a non-blank bitmap`() {
        val bitmap = renderer.render(
            matrix(),
            QrStyle(eyeShape = EyeShape.ROUNDED),
            256,
        )
        assertTrue(hasNonWhitePixels(bitmap))
    }

    // ---- Determinism ------------------------------------------------

    @Test
    fun `rendering the same inputs twice produces identical bitmaps`() {
        val m = matrix()
        val style = QrStyle(moduleShape = ModuleShape.ROUNDED, eyeShape = EyeShape.ROUNDED)
        val first = renderer.render(m, style, 256)
        val second = renderer.render(m, style, 256)
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        val px1 = IntArray(first.width * first.height)
        val px2 = IntArray(second.width * second.height)
        first.getPixels(px1, 0, first.width, 0, 0, first.width, first.height)
        second.getPixels(px2, 0, second.width, 0, 0, second.width, second.height)
        assertTrue("pixel arrays must be identical", px1.contentEquals(px2))
    }

    // ---- Round-trip -------------------------------------------------

    @Test
    fun `rendered bitmap passes the scannability verifier`() {
        val content = QrContent.Text("renderer round-trip")
        val result = engine.generate(content, EccLevel.M) as QrGenerationResult.Success
        val bitmap = renderer.render(result.matrix, QrStyle(), 512)
        assertTrue(
            BitmapScannabilityVerifier.isScannable(bitmap, result.payload),
        )
    }

    @Test
    fun `dot module rendered bitmap passes the scannability verifier`() {
        val content = QrContent.Text(
            "dot module round-trip with a longer payload so the symbol is large enough",
        )
        val result = engine.generate(content, EccLevel.M) as QrGenerationResult.Success
        val bitmap = renderer.render(result.matrix, QrStyle(moduleShape = ModuleShape.DOT), 512)
        assertTrue(
            BitmapScannabilityVerifier.isScannable(bitmap, result.payload),
        )
    }

    // ---- Color output -----------------------------------------------

    @Test
    fun `custom foreground color is applied to the output`() {
        // Red foreground: at least one pixel should be predominantly red.
        val style = QrStyle(foregroundArgb = 0xFFFF0000L, backgroundArgb = 0xFFFFFFFFL)
        val bitmap = renderer.render(matrix(), style, 256)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val hasRedPixel = pixels.any { pixel ->
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            r > 200 && g < 50 && b < 50
        }
        assertTrue("Expected at least one red pixel in the output", hasRedPixel)
    }

    // ---- Helpers ----------------------------------------------------

    private fun hasNonWhitePixels(bitmap: android.graphics.Bitmap): Boolean {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.any { it != -1 } // -1 == 0xFFFFFFFF (opaque white)
    }
}
