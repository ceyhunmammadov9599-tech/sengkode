package com.hjinlabs.sengkode.core.style

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.FrameSpec
import com.hjinlabs.sengkode.core.model.LogoSpec
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.type
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.WifiEncryption
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

/**
 * THE Phase 2 quality gate: for every styled render, generate ->
 * style -> paint (JVM backend) -> decode must reproduce the exact
 * payload. A style that fails decoding fails this suite; the suite
 * is never weakened for a visual feature.
 */
class StyledRoundTripTest {

    private val engine = ZxingQrEngine()
    private val renderer = QrStyleRenderer()
    private val size = 512

    // -- color presets -------------------------------------------------

    @Test
    fun `every shipped color preset decodes`() {
        StylePresets.COLOR_PRESETS.forEach { preset ->
            roundTrip(
                content = QrContent.Text("SENGKODE preset ${preset.name}"),
                style = QrStyle(
                    foregroundArgb = preset.foregroundArgb,
                    backgroundArgb = preset.backgroundArgb,
                ),
                ecc = EccLevel.M,
            )
        }
    }

    // -- module shapes x eye shapes ------------------------------------

    @Test
    fun `every module shape with every eye shape decodes`() {
        val content = QrContent.Url("hjinlabs.app")
        ModuleShape.entries.forEach { module ->
            EyeShape.entries.forEach { eye ->
                roundTrip(
                    content = content,
                    style = QrStyle(moduleShape = module, eyeShape = eye),
                    ecc = EccLevel.M,
                )
            }
        }
    }

    // -- ECC levels with styling ---------------------------------------

    @Test
    fun `styled codes decode at every ecc level`() {
        val content = QrContent.Text("SENGKODE ECC sweep")
        EccLevel.entries.forEach { level ->
            roundTrip(
                content = content,
                style = QrStyle(moduleShape = ModuleShape.ROUNDED, eyeShape = EyeShape.ROUNDED),
                ecc = level,
            )
        }
    }

    // -- logo ----------------------------------------------------------

    @Test
    fun `logos at several sizes decode`() {
        val logo = syntheticLogo()
        listOf(0.12f, 0.18f, 0.25f).forEach { fraction ->
            roundTrip(
                // Long enough payload for a version >= 2 symbol: the
                // scan-safety policy bounds logos against symbol size.
                content = QrContent.Text("SENGKODE logo coverage $fraction 0123456789"),
                style = QrStyle(logo = LogoSpec(sizeFraction = fraction)),
                ecc = LogoEccPolicy.effectiveEcc(EccLevel.M, LogoSpec(fraction)),
                logo = logo,
            )
        }
    }

    @Test
    fun `logo with dot modules decodes`() {
        roundTrip(
            content = QrContent.Text("SENGKODE dotted logo 0123456789"),
            style = QrStyle(
                moduleShape = ModuleShape.DOT,
                logo = LogoSpec(sizeFraction = 0.15f),
            ),
            ecc = EccLevel.Q,
            logo = syntheticLogo(),
        )
    }

    // -- frames ----------------------------------------------------------

    @Test
    fun `scan me frames decode`() {
        roundTrip(
            content = QrContent.Text("SENGKODE framed"),
            style = QrStyle(frame = FrameSpec()),
            ecc = EccLevel.M,
        )
    }

    @Test
    fun `frame plus logo decodes`() {
        roundTrip(
            content = QrContent.Text("SENGKODE framed logo 0123456789"),
            style = QrStyle(
                frame = FrameSpec(),
                moduleShape = ModuleShape.ROUNDED,
                eyeShape = EyeShape.ROUNDED,
                logo = LogoSpec(sizeFraction = 0.18f),
            ),
            ecc = LogoEccPolicy.effectiveEcc(EccLevel.M, LogoSpec(0.18f)),
            logo = syntheticLogo(),
        )
    }

    // -- content types with styling -------------------------------------

    @Test
    fun `styled wifi vcard and geo decode`() {
        roundTrip(
            content = QrContent.Wifi("HomeNet", "secret123", WifiEncryption.WPA, true),
            style = QrStyle(moduleShape = ModuleShape.DOT, eyeColorArgb = 0xFF00695CL),
            ecc = EccLevel.Q,
        )
        roundTrip(
            content = QrContent.VCard(
                "Jeyhun Mammadov", "HJIN Labs", "+994 50 123 45 67",
                "jeyhun@hjinlabs.app", "https://hjinlabs.app", "Hello",
            ),
            style = QrStyle(eyeShape = EyeShape.ROUNDED, eyeColorArgb = 0xFFB3252BL),
            ecc = EccLevel.Q,
        )
        roundTrip(
            content = QrContent.Geo(1.3521, 103.8198),
            style = QrStyle(moduleShape = ModuleShape.ROUNDED),
            ecc = EccLevel.M,
        )
    }

    // -- unicode ----------------------------------------------------------

    @Test
    fun `unicode content with styling decodes`() {
        roundTrip(
            content = QrContent.Text("SENGKODE — Singapura 码 🇸🇬 0123"),
            style = QrStyle(moduleShape = ModuleShape.ROUNDED, eyeShape = EyeShape.ROUNDED),
            ecc = EccLevel.M,
        )
    }

    @Test
    fun `tinted backgrounds decode`() {
        // Light backgrounds with dark modules (never inverted).
        // NOTE: an earlier draft used emerald modules on F1F8E9 -
        // the policy refused it at 4.4:1 contrast, exactly the kind
        // of borderline pair this gate exists to catch.
        listOf(0xFFFFF8E1L, 0xFFECEFF1L, 0xFFF1F8E9L).forEach { bg ->
            roundTrip(
                content = QrContent.Text("SENGKODE background sweep"),
                // Charcoal modules: neutral-dark against every tint,
                // so the sweep varies the BACKGROUND only.
                style = QrStyle(foregroundArgb = 0xFF263238L, backgroundArgb = bg),
                ecc = EccLevel.M,
            )
        }
    }

    // -- harness ----------------------------------------------------------

    private var cases = 0

    private fun roundTrip(
        content: QrContent,
        style: QrStyle,
        ecc: EccLevel,
        logo: LogoImage? = null,
    ) {
        cases++
        val verdict = ScanSafetyPolicy.evaluate(
            style,
            matrixWidthGuess(content),
        )
        assertTrue(
            "style must be policy-safe before it enters the gate: $verdict",
            verdict.isSafe,
        )
        val result = engine.generate(content, ecc)
        assertTrue("generation failed: $result", result is QrGenerationResult.Success)
        result as QrGenerationResult.Success

        val ops = renderer.render(result.matrix, style, size, logo)
        val decoded = decode(paint(ops, size))
        assertEquals(
            "styled decode mismatch for ${content.type} with $style",
            result.payload,
            decoded,
        )
    }

    /**
     * Policy width for pre-check: the engine decides the real version;
     * a version-2 estimate is the conservative common case for the
     * payloads used here (checked against the actual matrix below).
     */
    private fun matrixWidthGuess(content: QrContent): Int {
        val result = engine.generate(content, EccLevel.M)
        return if (result is QrGenerationResult.Success) result.matrix.width else 25
    }

    private fun syntheticLogo(): LogoImage {
        // Checkerboard: visually solid, deterministic.
        val w = 24
        val pixels = IntArray(w * w) { i ->
            val x = i % w
            val y = i / w
            if ((x / 3 + y / 3) % 2 == 0) 0xFF000000.toInt() else 0xFF00695C.toInt()
        }
        return LogoImage(pixels, w, w)
    }

    // -- JVM draw-list backend (test-only; mirrors :core:export) ------

    private fun paint(ops: List<DrawOp>, sizePx: Int): BufferedImage {
        val image = BufferedImage(sizePx, sizePx, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        for (op in ops) {
            when (op) {
                is DrawOp.FillRect -> {
                    g.color = Color(op.argb.toInt(), true)
                    g.fillRect(
                        op.x.roundToInt(), op.y.roundToInt(),
                        op.w.roundToInt(), op.h.roundToInt(),
                    )
                }
                is DrawOp.RoundRect -> {
                    g.color = Color(op.argb.toInt(), true)
                    g.fill(
                        RoundRectangle2D.Float(
                            op.x, op.y, op.w, op.h, op.radiusPx, op.radiusPx,
                        ),
                    )
                }
                is DrawOp.Circle -> {
                    g.color = Color(op.argb.toInt(), true)
                    g.fillOval(
                        (op.cx - op.radius).roundToInt(),
                        (op.cy - op.radius).roundToInt(),
                        (op.radius * 2).roundToInt(),
                        (op.radius * 2).roundToInt(),
                    )
                }
                is DrawOp.StrokeRoundRect -> {
                    g.color = Color(op.argb.toInt(), true)
                    g.stroke = BasicStroke(op.strokePx)
                    g.draw(
                        RoundRectangle2D.Float(
                            op.x, op.y, op.w, op.h, op.radiusPx, op.radiusPx,
                        ),
                    )
                }
                is DrawOp.Bitmap -> blit(g, op)
                is DrawOp.Text -> {
                    // Label text sits in the frame band, outside the
                    // symbol - skipped on purpose for the decode gate.
                }
            }
        }
        g.dispose()
        return image
    }

    private fun blit(g: Graphics2D, op: DrawOp.Bitmap) {
        // Deterministic nearest-neighbor scaling.
        val outW = op.w.roundToInt().coerceAtLeast(1)
        val outH = op.h.roundToInt().coerceAtLeast(1)
        for (ty in 0 until outH) {
            for (tx in 0 until outW) {
                val sx = tx * op.pixelWidth / outW
                val sy = ty * op.pixelHeight / outH
                g.color = Color(op.pixels[sy * op.pixelWidth + sx], true)
                g.fillRect(op.x.roundToInt() + tx, op.y.roundToInt() + ty, 1, 1)
            }
        }
    }

    private fun decode(image: BufferedImage): String {
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        val source = RGBLuminanceSource(image.width, image.height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
        return QRCodeReader().decode(bitmap, hints).text
    }
}
