package com.hjinlabs.sengkode.core.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.hjinlabs.sengkode.core.style.DrawOp
import com.hjinlabs.sengkode.core.style.LogoImage
import com.hjinlabs.sengkode.core.style.QrStyleRenderer
import com.hjinlabs.sengkode.core.model.QrMatrix
import com.hjinlabs.sengkode.core.model.QrStyle
import kotlin.math.roundToInt

/**
 * Android backend for the style pipeline: interprets the pure
 * [DrawOp] list onto an ARGB_8888 Bitmap. The preview and the
 * exported PNG run through THIS ONE backend, so preview == export
 * structurally - same geometry, same colors, same file.
 */
class DrawListBitmapRenderer(
    private val styleRenderer: QrStyleRenderer = QrStyleRenderer(),
) {

    fun render(
        matrix: QrMatrix,
        style: QrStyle,
        sizePx: Int,
        logo: LogoImage? = null,
    ): Bitmap {
        require(sizePx > 0) { "sizePx must be positive, was $sizePx" }
        val ops = styleRenderer.render(matrix, style, sizePx, logo)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (op in ops) {
            when (op) {
                is DrawOp.FillRect -> {
                    paint.style = Paint.Style.FILL
                    paint.color = op.argb.toInt()
                    canvas.drawRect(op.x, op.y, op.x + op.w, op.y + op.h, paint)
                }
                is DrawOp.RoundRect -> {
                    paint.style = Paint.Style.FILL
                    paint.color = op.argb.toInt()
                    canvas.drawRoundRect(
                        RectF(op.x, op.y, op.x + op.w, op.y + op.h),
                        op.radiusPx, op.radiusPx, paint,
                    )
                }
                is DrawOp.Circle -> {
                    paint.style = Paint.Style.FILL
                    paint.color = op.argb.toInt()
                    canvas.drawCircle(op.cx, op.cy, op.radius, paint)
                }
                is DrawOp.StrokeRoundRect -> {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = op.strokePx
                    paint.color = op.argb.toInt()
                    canvas.drawRoundRect(
                        RectF(op.x, op.y, op.x + op.w, op.y + op.h),
                        op.radiusPx, op.radiusPx, paint,
                    )
                }
                is DrawOp.Bitmap -> blit(canvas, paint, op)
                is DrawOp.Text -> {
                    paint.style = Paint.Style.FILL
                    paint.color = op.argb.toInt()
                    paint.textSize = op.sizePx
                    paint.isAntiAlias = true
                    val textWidth = paint.measureText(op.text)
                    canvas.drawText(op.text, op.centerX - textWidth / 2f, op.baselineY, paint)
                }
            }
        }
        return bitmap
    }

    /**
     * Nearest-neighbor logo blit - deterministic and identical to the
     * JVM test backend's scaling, so the decode gate's verdict carries
     * over to this backend pixel for pixel (module geometry).
     */
    private fun blit(canvas: Canvas, paint: Paint, op: DrawOp.Bitmap) {
        val outW = op.w.roundToInt().coerceAtLeast(1)
        val outH = op.h.roundToInt().coerceAtLeast(1)
        val scaled = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        for (ty in 0 until outH) {
            for (tx in 0 until outW) {
                val sx = tx * op.pixelWidth / outW
                val sy = ty * op.pixelHeight / outH
                scaled.setPixel(tx, ty, op.pixels[sy * op.pixelWidth + sx])
            }
        }
        paint.style = Paint.Style.FILL
        canvas.drawBitmap(scaled, op.x, op.y, paint)
    }
}
