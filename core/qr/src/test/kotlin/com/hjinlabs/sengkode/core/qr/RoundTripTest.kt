package com.hjinlabs.sengkode.core.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.type
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrMatrix
import com.hjinlabs.sengkode.core.model.WifiEncryption
import java.awt.image.BufferedImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * THE quality gate of the product: every generated matrix must decode
 * back to exactly the payload the engine encoded. ZXing's QRCodeReader
 * plays the role of a reference scanner - all on the JVM, no emulator.
 */
class RoundTripTest {

    private val engine = ZxingQrEngine()

    @Test
    fun `text qr round-trips at every ecc level`() {
        EccLevel.entries.forEach { level ->
            val payload = "SENGKODE round trip at ${level.name}"
            roundTrip(QrContent.Text(payload), level, expected = payload)
        }
    }

    @Test
    fun `url round-trip`() {
        roundTrip(QrContent.Url("hjinlabs.app"), EccLevel.M, expected = "https://hjinlabs.app")
    }

    @Test
    fun `wifi round-trip with special characters`() {
        roundTrip(
            QrContent.Wifi("Cafe; \\Best", "pa;ss:word123", WifiEncryption.WPA, true),
            EccLevel.Q,
            expected = "WIFI:T:WPA;S:Cafe\\; \\\\Best;P:pa\\;ss\\:word123;H:true;;",
        )
    }

    @Test
    fun `vcard round-trip`() {
        val payload = VCardEncoder.payload(
            QrContent.VCard(
                fullName = "Jeyhun Mammadov",
                organization = "HJIN Labs",
                phone = "+994 50 123 45 67",
                email = "jeyhun@hjinlabs.app",
                website = "https://hjinlabs.app",
                note = "Hello",
            ),
        )
        roundTrip(
            QrContent.VCard(
                "Jeyhun Mammadov", "HJIN Labs",
                "+994 50 123 45 67", "jeyhun@hjinlabs.app",
                "https://hjinlabs.app", "Hello",
            ),
            EccLevel.M,
            expected = payload,
        )
    }

    @Test
    fun `email round-trip`() {
        roundTrip(
            QrContent.Email("hi@example.com", "Hello there", "How are you?"),
            EccLevel.M,
            expected = "mailto:hi@example.com?subject=Hello%20there&body=How%20are%20you%3F",
        )
    }

    @Test
    fun `sms round-trip`() {
        roundTrip(
            QrContent.Sms("+15551234567", "Hello!"),
            EccLevel.M,
            expected = "SMSTO:+15551234567:Hello!",
        )
    }

    @Test
    fun `phone round-trip`() {
        roundTrip(QrContent.Phone("+15551234567"), EccLevel.M, expected = "TEL:+15551234567")
    }

    @Test
    fun `geo round-trip`() {
        roundTrip(
            QrContent.Geo(1.3521, 103.8198),
            EccLevel.M,
            expected = "GEO:1.3521,103.8198",
        )
    }

    @Test
    fun `unicode round-trip`() {
        val unicode = "SENGKODE — Singapura 码 🇸🇬"
        roundTrip(QrContent.Text(unicode), EccLevel.M, expected = unicode)
    }

    @Test
    fun `long realistic payload round-trips`() {
        val long = buildString {
            repeat(60) { append("SENGKODE offline-first privacy QR. ") }
        }
        roundTrip(QrContent.Text(long), EccLevel.L, expected = long)
    }

    // -- harness ------------------------------------------------------

    private fun roundTrip(content: QrContent, level: EccLevel, expected: String) {
        val result = engine.generate(content, level)
        assertTrue(
            "generation failed for ${content.type} at ${level.name}: $result",
            result is QrGenerationResult.Success,
        )
        result as QrGenerationResult.Success
        assertEquals(expected, result.payload)
        assertEquals(expected, decode(result.matrix))
    }

    /**
     * Renders the pure matrix to a binary BufferedImage and decodes it
     * with ZXing's QRCodeReader as a reference scanner would.
     */
    private fun decode(matrix: QrMatrix): String {
        val scale = 4 // render modules 4px so the binarizer has margin
        val image = BufferedImage(
            matrix.width * scale,
            matrix.height * scale,
            BufferedImage.TYPE_BYTE_BINARY,
        )
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                val on = matrix.isSet(x, y)
                for (dy in 0 until scale) {
                    for (dx in 0 until scale) {
                        image.setRGB(
                            x * scale + dx,
                            y * scale + dy,
                            if (on) 0xFF000000.toInt() else 0xFFFFFFFF.toInt(),
                        )
                    }
                }
            }
        }
        val pixels = IntArray(image.width * image.height)
        image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
        val source = RGBLuminanceSource(image.width, image.height, pixels)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        val hints = mapOf(DecodeHintType.CHARACTER_SET to Charsets.UTF_8.name())
        return QRCodeReader().decode(bitmap, hints).text
    }
}
