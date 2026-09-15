package com.hjinlabs.sengkode.core.qr

import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrError
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.WifiEncryption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrEngineTest {

    private val engine = ZxingQrEngine()

    @Test
    fun `text content generates a success with payload and version`() {
        val result = engine.generate(QrContent.Text("SENGKODE"), EccLevel.M)
        assertTrue(result is QrGenerationResult.Success)
        result as QrGenerationResult.Success
        assertEquals("SENGKODE", result.payload)
        assertEquals(EccLevel.M, result.eccLevel)
        assertTrue(result.version in 1..40)
        assertTrue(result.matrix.width == result.matrix.height)
    }

    @Test
    fun `invalid content returns a typed validation error - never throws`() {
        val result = engine.generate(QrContent.Text(""), EccLevel.M)
        assertTrue(result is QrGenerationResult.Failure)
        assertTrue((result as QrGenerationResult.Failure).error is QrError.Validation)
    }

    @Test
    fun `oversized content returns a typed capacity error`() {
        val result = engine.generate(QrContent.Text("x".repeat(2400)), EccLevel.M)
        assertTrue(result is QrGenerationResult.Failure)
        val error = (result as QrGenerationResult.Failure).error
        assertTrue(error is QrError.Capacity)
        error as QrError.Capacity
        assertEquals(2400, error.payloadBytes)
        assertEquals(2331, error.maxBytes)
    }

    @Test
    fun `same payload at lower ecc succeeds where higher failed`() {
        val big = QrContent.Text("x".repeat(1274))
        assertTrue(engine.generate(big, EccLevel.H) is QrGenerationResult.Failure)
        assertTrue(engine.generate(big, EccLevel.L) is QrGenerationResult.Success)
    }

    @Test
    fun `unicode text round-trips through the engine payload`() {
        val content = "Jeyhun — Šingapūra 码 العدد 🇸🇬"
        val result = engine.generate(QrContent.Text(content), EccLevel.M)
        assertTrue(result is QrGenerationResult.Success)
        assertEquals(content, (result as QrGenerationResult.Success).payload)
    }

    @Test
    fun `wifi content full pipeline`() {
        val result = engine.generate(
            QrContent.Wifi("HomeNet", "secret123", WifiEncryption.WPA, true),
            EccLevel.Q,
        )
        assertTrue(result is QrGenerationResult.Success)
        assertEquals(
            "WIFI:T:WPA;S:HomeNet;P:secret123;H:true;;",
            (result as QrGenerationResult.Success).payload,
        )
    }

    @Test
    fun `url normalization is applied before generation`() {
        val result = engine.generate(QrContent.Url("hjinlabs.app"), EccLevel.M)
        assertTrue(result is QrGenerationResult.Success)
        assertEquals(
            "https://hjinlabs.app",
            (result as QrGenerationResult.Success).payload,
        )
    }
}
