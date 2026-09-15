package com.hjinlabs.sengkode.core.qr

import com.hjinlabs.sengkode.core.model.EccLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class CapacityTest {

    @Test
    fun `small payloads pass at every level`() {
        EccLevel.entries.forEach { level ->
            assertNull(Capacity.check("hello", level))
        }
    }

    @Test
    fun `spec byte capacities are correct`() {
        assertEquals(2953, Capacity.maxPayloadBytes(EccLevel.L))
        assertEquals(2331, Capacity.maxPayloadBytes(EccLevel.M))
        assertEquals(1863, Capacity.maxPayloadBytes(EccLevel.Q))
        assertEquals(1273, Capacity.maxPayloadBytes(EccLevel.H))
    }

    @Test
    fun `oversized payload reports a reason with numbers`() {
        val big = "x".repeat(1274)
        val reason = Capacity.check(big, EccLevel.H)
        assertNotNull(reason)
        assertTrue(reason!!.contains("1274"))
        assertTrue(reason.contains("1273"))
    }

    @Test
    fun `payload fitting L but not H is level-aware`() {
        val fitsLnotH = "x".repeat(1274) // 1274 bytes: > H (1273), <= L (2953)
        assertNull(Capacity.check(fitsLnotH, EccLevel.L))
        assertNotNull(Capacity.check(fitsLnotH, EccLevel.H))
    }

    @Test
    fun `multi-byte characters count as their utf8 size`() {
        // 700 CJK chars = 2100 UTF-8 bytes: fits M (2331), not H (1273)
        val cjk = "码".repeat(700)
        assertNull(Capacity.check(cjk, EccLevel.M))
        assertNotNull(Capacity.check(cjk, EccLevel.H))
    }
}
