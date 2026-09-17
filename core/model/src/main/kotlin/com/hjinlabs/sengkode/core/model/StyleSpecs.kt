package com.hjinlabs.sengkode.core.model

import kotlinx.serialization.Serializable

/**
 * Phase 2 customization spec types. Pure Kotlin, immutable, additive to
 * the Phase 1 model - every field has a default that reproduces the
 * classic black-on-white QR (backward compatibility by construction).
 */

/** Data-module rendering shapes. */
@Serializable
enum class ModuleShape { SQUARE, ROUNDED, DOT }

/** Finder-pattern ("eye") rendering shapes. */
@Serializable
enum class EyeShape { SQUARE, ROUNDED }

/**
 * Center-logo placement spec. [sizeFraction] is the logo's side
 * relative to the QR symbol side (not the whole canvas) and is
 * bounded by the scan-safety policy: the logo may never reach a
 * finder pattern and its coverage drives the required ECC level.
 * The actual logo PIXELS are a render-time input (see :core:style
 * LogoImage) - bitmaps do not belong in the domain model.
 */
@Serializable
data class LogoSpec(val sizeFraction: Float = DEFAULT_FRACTION) {
    companion object {
        const val DEFAULT_FRACTION = 0.18f
        const val MIN_FRACTION = 0.10f
        const val MAX_FRACTION = 0.25f
    }
}

/** High-quality frame presets; frames live OUTSIDE the symbol. */
@Serializable
enum class FrameStyle { BORDER }

/**
 * Frame configuration. The frame adds padding around the QR symbol
 * (quiet zone included) and never covers it, reduces the scan area,
 * or modifies modules.
 */
@Serializable
data class FrameSpec(
    val style: FrameStyle = FrameStyle.BORDER,
    val labelText: String = DEFAULT_LABEL,
) {
    companion object {
        const val DEFAULT_LABEL = "SCAN ME"
    }
}
