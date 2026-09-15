package com.hjinlabs.sengkode.core.qr

import com.hjinlabs.sengkode.core.model.EccLevel

/**
 * QR capacity validation. Byte-mode capacity at symbol version 40 per
 * ISO/IEC 18004 (the only values the pre-check needs: if a payload fits
 * its level at version 40, ZXing will select a suitable smaller
 * version; if not, no version can hold it).
 */
object Capacity {

    private val MAX_BYTES = mapOf(
        EccLevel.L to 2953,
        EccLevel.M to 2331,
        EccLevel.Q to 1863,
        EccLevel.H to 1273,
    )

    fun maxPayloadBytes(level: EccLevel): Int = MAX_BYTES.getValue(level)

    /** Returns null when the payload fits, or a friendly reason when not. */
    fun check(payload: String, level: EccLevel): String? {
        val bytes = payload.toByteArray(Charsets.UTF_8).size
        val max = maxPayloadBytes(level)
        if (bytes <= max) return null
        return "This content is $bytes bytes but a QR code at error correction " +
            "${level.name} holds at most $max bytes. Shorten the content or choose " +
            "a lower error correction level."
    }
}
