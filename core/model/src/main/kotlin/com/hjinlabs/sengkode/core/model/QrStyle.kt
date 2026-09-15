package com.hjinlabs.sengkode.core.model

/**
 * Visual configuration for rendering a QR matrix.
 *
 * Phase 1 scope: only the essentials the renderer needs (module and
 * background colors, quiet-zone size). The full customization studio
 * (shapes, logos, frames) is a Phase 2 deliverable and will EXTEND this
 * class; the rendering contract (matrix in -> bitmap out) stays stable.
 */
data class QrStyle(
    /** ARGB color of the dark modules. */
    val foregroundArgb: Long = 0xFF000000L,
    /** ARGB color of the light modules and quiet zone. */
    val backgroundArgb: Long = 0xFFFFFFFFL,
    /** Quiet-zone width in module units (spec minimum: 4). */
    val quietZoneModules: Int = DEFAULT_QUIET_ZONE,
) {
    companion object {
        const val DEFAULT_QUIET_ZONE = 4
    }
}

/** QR error-correction levels. Default across the app is [M]. */
enum class EccLevel {
    L, M, Q, H;

    companion object {
        val DEFAULT = M
    }
}
