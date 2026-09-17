package com.hjinlabs.sengkode.core.database

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.WifiEncryption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The persistence codec contract: every content type and style
 * snapshot must survive a JSON round trip EXACTLY - including the
 * sensitive payloads (Wi-Fi passwords) history stores.
 */
class SnapshotCodecTest {

    private val allContents = listOf(
        QrContent.Text("SENGKODE"),
        QrContent.Url("https://hjinlabs.app"),
        QrContent.Wifi("HomeNet", "p@ss w:ord;\\x", WifiEncryption.WPA, true),
        QrContent.VCard(
            "Jeyhun Mammadov", "HJIN Labs", "+994 50 123 45 67",
            "jeyhun@hjinlabs.app", "https://hjinlabs.app", "note, with; chars",
        ),
        QrContent.Email("hi@hjinlabs.app", "Subject", "Body text"),
        QrContent.Sms("+15551234567", "Hello"),
        QrContent.Phone("+994501234567"),
        QrContent.Geo(1.3521, 103.8198),
    )

    @Test
    fun `every content type round trips exactly`() {
        allContents.forEach { content ->
            val decoded = SnapshotCodec.decodeContent(SnapshotCodec.encodeContent(content))
            assertEquals(content, decoded)
        }
    }

    @Test
    fun `every style configuration round trips exactly`() {
        val styles = listOf(
            QrStyle(),
            com.hjinlabs.sengkode.core.style.BuiltInTemplates.ALL
                .map { it.style }
                .first { it.frame != null },
            QrStyle(
                moduleShape = com.hjinlabs.sengkode.core.model.ModuleShape.DOT,
                eyeShape = com.hjinlabs.sengkode.core.model.EyeShape.ROUNDED,
                eyeColorArgb = 0xFFB3252BL,
                logo = com.hjinlabs.sengkode.core.model.LogoSpec(sizeFraction = 0.20f),
                frame = com.hjinlabs.sengkode.core.model.FrameSpec(labelText = "SCAN ME"),
            ),
        )
        styles.forEach { style ->
            assertEquals(style, SnapshotCodec.decodeStyle(SnapshotCodec.encodeStyle(style)))
        }
    }

    @Test
    fun `codec tolerates unknown keys (forward compatibility)`() {
        val json = SnapshotCodec.encodeContent(QrContent.Text("hi")).trimEnd()
        val withExtra = json.dropLast(1) + ",\"futureField\":42}"
        assertEquals(QrContent.Text("hi"), SnapshotCodec.decodeContent(withExtra))
    }

    @Test
    fun `wifi passwords are stored verbatim not escaped away`() {
        val wifi = QrContent.Wifi("Net", "secret;pass\\word", WifiEncryption.WEP, false)
        val decoded = SnapshotCodec.decodeContent(SnapshotCodec.encodeContent(wifi))
        assertTrue(decoded is QrContent.Wifi)
        assertEquals("secret;pass\\word", (decoded as QrContent.Wifi).password)
    }
}
