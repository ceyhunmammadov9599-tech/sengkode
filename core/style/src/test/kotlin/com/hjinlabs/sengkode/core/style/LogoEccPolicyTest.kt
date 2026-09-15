package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.LogoSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoEccPolicyTest {

    @Test
    fun `no logo keeps the requested level`() {
        assertEquals(EccLevel.M, LogoEccPolicy.effectiveEcc(EccLevel.M, null))
        assertEquals(EccLevel.L, LogoEccPolicy.effectiveEcc(EccLevel.L, null))
    }

    @Test
    fun `small logo upgrades to Q`() {
        assertEquals(
            EccLevel.Q,
            LogoEccPolicy.effectiveEcc(EccLevel.M, LogoSpec(sizeFraction = 0.12f)),
        )
    }

    @Test
    fun `large logo upgrades to H`() {
        assertEquals(
            EccLevel.H,
            LogoEccPolicy.effectiveEcc(EccLevel.M, LogoSpec(sizeFraction = 0.20f)),
        )
    }

    @Test
    fun `requested level is a floor never lowered`() {
        assertEquals(
            EccLevel.H,
            LogoEccPolicy.effectiveEcc(EccLevel.H, LogoSpec(sizeFraction = 0.12f)),
        )
        assertEquals(
            EccLevel.Q,
            LogoEccPolicy.effectiveEcc(EccLevel.Q, LogoSpec(sizeFraction = 0.12f)),
        )
    }

    @Test
    fun `boundary fraction 18 percent maps to Q`() {
        assertEquals(
            EccLevel.Q,
            LogoEccPolicy.effectiveEcc(EccLevel.L, LogoSpec(sizeFraction = 0.18f)),
        )
        assertEquals(
            EccLevel.H,
            LogoEccPolicy.effectiveEcc(EccLevel.L, LogoSpec(sizeFraction = 0.19f)),
        )
    }
}
