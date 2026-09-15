package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.LogoSpec

/**
 * Logo-aware ECC selection. Coverage-based, capacity-aware:
 * - No logo -> the user's level stands.
 * - Logo up to 18% of the symbol -> Q.
 * - Larger logo -> H.
 * The result is a MINIMUM: a user-requested higher level is kept.
 * If the payload then exceeds the level's capacity, the engine's
 * typed Capacity error reaches the user - never a silent downgrade
 * to an unsafe configuration.
 */
object LogoEccPolicy {

    const val Q_FRACTION_LIMIT = 0.18f

    fun effectiveEcc(requested: EccLevel, logo: LogoSpec?): EccLevel {
        if (logo == null) return requested
        val required = if (logo.sizeFraction <= Q_FRACTION_LIMIT) EccLevel.Q else EccLevel.H
        return if (required.ordinal > requested.ordinal) required else requested
    }
}
