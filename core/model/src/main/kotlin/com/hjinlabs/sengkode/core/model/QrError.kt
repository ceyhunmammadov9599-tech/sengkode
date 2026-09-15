package com.hjinlabs.sengkode.core.model

/**
 * Typed, user-presentable failure taxonomy for the whole generation
 * pipeline. No raw engine/ZXing exception ever crosses this boundary.
 */
sealed interface QrError {

    /** Every error carries a human-readable, ready-to-present message. */
    val userMessage: String

    /** The content is incomplete or semantically invalid. */
    data class Validation(override val userMessage: String) : QrError

    /**
     * The payload does not fit the QR specification at the requested
     * error-correction level. Carries the numbers so the UI can present
     * a precise, actionable message.
     */
    data class Capacity(
        override val userMessage: String,
        val payloadBytes: Int,
        val maxBytes: Int,
        val eccLevel: EccLevel,
    ) : QrError

    /** The engine rejected the payload while encoding (never silent). */
    data class Encoding(override val userMessage: String) : QrError
}
