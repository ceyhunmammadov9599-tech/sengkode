package com.hjinlabs.sengkode.core.style

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import kotlin.math.roundToInt

/**
 * Shared JVM draw-list painter + decoder used by the styled round-trip
 * suites. Mirrors the Android backend's geometry exactly.
 */
object RoundTripHarness {

    fun paint(ops: List<DrawOp>, sizePx: Int): BufferedImage {
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
                        RoundRectangle2D.Float(op.x, op.y, op.w, op.h, op.radiusPx, op.radiusPx),
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
                        RoundRectangle2D.Float(op.x, op.y, op.w, op.h, op.radiusPx, op.radiusPx),
                    )
                }
                is DrawOp.Bitmap -> blit(g, op)
                is DrawOp.Text -> Unit // outside the symbol; cosmetic only
            }
        }
        g.dispose()
        return image
    }

    private fun blit(g: Graphics2D, op: DrawOp.Bitmap) {
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

    fun decode(image: BufferedImage): String {
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        val source = RGBLuminanceSource(image.width, image.height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
        return QRCodeReader().decode(bitmap, hints).text
    }
}
