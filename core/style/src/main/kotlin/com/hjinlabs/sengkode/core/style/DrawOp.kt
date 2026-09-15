package com.hjinlabs.sengkode.core.style

/**
 * Backend-independent drawing primitives. The styled renderer emits
 * this list; the Android backend (:core:export) and the JVM test
 * backend interpret it. One geometry, every surface - which is how
 * preview == export is guaranteed structurally, not by luck.
 * Coordinates are output pixels.
 */
sealed interface DrawOp {
    data class FillRect(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val argb: Long,
    ) : DrawOp

    data class RoundRect(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val radiusPx: Float,
        val argb: Long,
    ) : DrawOp

    data class Circle(
        val cx: Float,
        val cy: Float,
        val radius: Float,
        val argb: Long,
    ) : DrawOp

    data class StrokeRoundRect(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val radiusPx: Float,
        val strokePx: Float,
        val argb: Long,
    ) : DrawOp

    /**
     * Bitmap blit (logo). [pixels] is ARGB row-major. The JVM test
     * backend scales it with nearest-neighbor so decode results are
     * fully deterministic.
     */
    data class Bitmap(
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val pixels: IntArray,
        val pixelWidth: Int,
        val pixelHeight: Int,
    ) : DrawOp {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Bitmap
            return x == other.x && y == other.y && w == other.w && h == other.h &&
                pixels.contentEquals(other.pixels) &&
                pixelWidth == other.pixelWidth && pixelHeight == other.pixelHeight
        }

        override fun hashCode(): Int {
            var result = x.hashCode()
            result = 31 * result + y.hashCode()
            result = 31 * result + w.hashCode()
            result = 31 * result + h.hashCode()
            result = 31 * result + pixels.contentHashCode()
            result = 31 * result + pixelWidth
            result = 31 * result + pixelHeight
            return result
        }
    }

    /** Label text (frame). Never overlaps the QR symbol. */
    data class Text(
        val text: String,
        val centerX: Float,
        val baselineY: Float,
        val sizePx: Float,
        val argb: Long,
    ) : DrawOp
}

/**
 * Logo pixels for rendering: ARGB row-major. Kept as an IntArray (not
 * a platform bitmap) so the pure renderer and both backends share it.
 */
class LogoImage(
    val pixels: IntArray,
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0 && height > 0 && pixels.size == width * height) {
            "Malformed logo image: ${pixels.size} px for ${width}x$height"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LogoImage
        return width == other.width && height == other.height &&
            pixels.contentEquals(other.pixels)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + pixels.contentHashCode()
        return result
    }
}
