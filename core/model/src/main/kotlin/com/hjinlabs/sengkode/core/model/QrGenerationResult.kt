package com.hjinlabs.sengkode.core.model

/**
 * Pure, framework-free representation of the generated QR matrix.
 * [bits] is row-major, [bits[y * width + x]] == true means a dark
 * module at (x, y). The quiet zone is NOT part of the matrix; the
 * renderer adds it (QrStyle.quietZoneModules).
 */
data class QrMatrix(
    val width: Int,
    val height: Int,
    val bits: BooleanArray,
) {
    init {
        require(width > 0 && height > 0 && bits.size == width * height) {
            "Malformed matrix: ${bits.size} bits for ${width}x$height"
        }
    }

    fun isSet(x: Int, y: Int): Boolean = bits[y * width + x]

    operator fun get(x: Int, y: Int): Boolean = isSet(x, y)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as QrMatrix
        return width == other.width && height == other.height && bits.contentEquals(other.bits)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + bits.contentHashCode()
        return result
    }
}

/**
 * Outcome of a full generation attempt. Success always carries the
 * payload actually encoded (post-normalization, e.g. URL scheme
 * prefixing) so the preview and export use exactly what will be
 * scanned.
 */
sealed interface QrGenerationResult {
    data class Success(
        val payload: String,
        val matrix: QrMatrix,
        val eccLevel: EccLevel,
        /** QR symbol version (1..40) the engine selected. */
        val version: Int,
    ) : QrGenerationResult

    data class Failure(val error: QrError) : QrGenerationResult
}
