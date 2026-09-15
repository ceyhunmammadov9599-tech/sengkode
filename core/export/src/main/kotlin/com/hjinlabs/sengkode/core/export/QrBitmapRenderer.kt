package com.hjinlabs.sengkode.core.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.hjinlabs.sengkode.core.model.QrMatrix
import com.hjinlabs.sengkode.core.model.QrStyle

/**
 * Renders a pure [QrMatrix] to an Android Bitmap at any resolution.
 * The matrix is the single source of truth; rendering adds the quiet
 * zone described by [QrStyle.quietZoneModules]. No Compose dependency
 * here - this is the same pipeline the Compose preview and the
 * exported PNG use, so what you see is exactly what is saved.
 */
class QrBitmapRenderer {

    fun render(matrix: QrMatrix, style: QrStyle, sizePx: Int): Bitmap {
        require(sizePx > 0) { "sizePx must be positive, was $sizePx" }
        val totalModules = matrix.width + 2 * style.quietZoneModules
        val cell = sizePx.toFloat() / totalModules
        val quiet = cell * style.quietZoneModules

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = style.backgroundArgb.toInt()
        canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)

        paint.color = style.foregroundArgb.toInt()
        var y = 0
        while (y < matrix.height) {
            var x = 0
            while (x < matrix.width) {
                if (matrix.isSet(x, y)) {
                    val left = quiet + x * cell
                    val top = quiet + y * cell
                    // Micro-inset guards against hairline seams between
                    // adjacent modules on non-integer cell sizes.
                    val inset = if (cell > 4f) 0.25f else 0f
                    canvas.drawRect(
                        left + inset,
                        top + inset,
                        left + cell - inset,
                        top + cell - inset,
                        paint,
                    )
                }
                x++
            }
            y++
        }
        return bitmap
    }
}
