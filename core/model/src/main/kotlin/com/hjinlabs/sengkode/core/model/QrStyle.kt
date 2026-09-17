package com.hjinlabs.sengkode.core.model

import kotlinx.serialization.Serializable

/**
 * Visual configuration for rendering a QR matrix. The Phase 1 fields
 * keep their defaults; the Phase 2 fields default to the classic
 * appearance, so existing call sites behave identically.
 *
 * Styling operates exclusively on RENDERING - the encoded matrix is
 * never modified for visual effects. The final scannability authority
 * is always the generate -> render -> decode round trip, not this
 * style object.
 */
@Serializable
data class QrStyle(
    /** ARGB color of the dark modules. */
    val foregroundArgb: Long = 0xFF000000L,
    /** ARGB color of the light modules and quiet zone. */
    val backgroundArgb: Long = 0xFFFFFFFFL,
    /** Quiet-zone width in module units (spec minimum: 4). */
    val quietZoneModules: Int = DEFAULT_QUIET_ZONE,
    /** Data-module rendering shape. */
    val moduleShape: ModuleShape = ModuleShape.SQUARE,
    /** Finder-pattern rendering shape. */
    val eyeShape: EyeShape = EyeShape.SQUARE,
    /** Finder color; null follows [foregroundArgb]. */
    val eyeColorArgb: Long? = null,
    /** Center logo; null disables the logo. */
    val logo: LogoSpec? = null,
    /** Frame; null disables the frame. */
    val frame: FrameSpec? = null,
) {
    /** Effective finder color (explicit or the module color). */
    val effectiveEyeColorArgb: Long
        get() = eyeColorArgb ?: foregroundArgb

    companion object {
        const val DEFAULT_QUIET_ZONE = 4
    }
}

/** QR error-correction levels. Default across the app is [M]. */
@Serializable
enum class EccLevel {
    L, M, Q, H;

    companion object {
        val DEFAULT = M
    }
}
