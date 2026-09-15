package com.hjinlabs.sengkode.core.qr

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.ValidationResult
import com.hjinlabs.sengkode.core.model.WifiEncryption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncodersTest {

    // ---- Text ------------------------------------------------------

    @Test
    fun `text payload is verbatim`() {
        assertEquals("Hello", TextEncoder.payload(QrContent.Text("Hello")))
    }

    @Test
    fun `blank text is invalid`() {
        val result = TextEncoder.validate(QrContent.Text("   "))
        assertTrue(result is ValidationResult.Invalid)
    }

    // ---- URL -------------------------------------------------------

    @Test
    fun `url without scheme gets https prefix`() {
        assertEquals(
            "https://example.com",
            UrlEncoder.payload(QrContent.Url("example.com")),
        )
    }

    @Test
    fun `url with http scheme is preserved`() {
        assertEquals(
            "http://example.com",
            UrlEncoder.payload(QrContent.Url("http://example.com")),
        )
    }

    @Test
    fun `blank url is invalid`() {
        assertTrue(UrlEncoder.validate(QrContent.Url(" ")) is ValidationResult.Invalid)
    }

    // ---- Wi-Fi -----------------------------------------------------

    @Test
    fun `wifi wpa payload format`() {
        assertEquals(
            "WIFI:T:WPA;S:HomeNet;P:secret123;;",
            WifiEncoder.payload(
                QrContent.Wifi("HomeNet", "secret123", WifiEncryption.WPA, false),
            ),
        )
    }

    @Test
    fun `wifi hidden flag and escaping`() {
        // ssid contains a backslash; password contains all escapable chars
        val payload = WifiEncoder.payload(
            QrContent.Wifi("My\\Net", "pa;ss,wo:rd\"xy", WifiEncryption.WPA, true),
        )
        assertEquals("WIFI:T:WPA;S:My\\\\Net;P:pa\\;ss\\,wo\\:rd\\\"xy;H:true;;", payload)
    }

    @Test
    fun `wifi nopass carries no password field`() {
        assertEquals(
            "WIFI:T:nopass;S:Cafe;;",
            WifiEncoder.payload(QrContent.Wifi("Cafe", "", WifiEncryption.NOPASS, false)),
        )
    }

    @Test
    fun `wpa password shorter than 8 is invalid`() {
        val result = WifiEncoder.validate(
            QrContent.Wifi("HomeNet", "short", WifiEncryption.WPA, false),
        )
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `wep key must be 5 or 13 chars`() {
        assertTrue(
            WifiEncoder.validate(QrContent.Wifi("Net", "1234", WifiEncryption.WEP, false))
                is ValidationResult.Invalid,
        )
        assertEquals(
            "WIFI:T:WEP;S:Net;P:12345;;",
            WifiEncoder.payload(QrContent.Wifi("Net", "12345", WifiEncryption.WEP, false)),
        )
    }

    // ---- vCard -----------------------------------------------------

    @Test
    fun `vcard payload structure`() {
        val payload = VCardEncoder.payload(
            QrContent.VCard(
                fullName = "Jeyhun Mammadov",
                organization = "HJIN Labs",
                phone = "+994 50 123 45 67",
                email = "jeyhun@hjinlabs.app",
                website = "https://hjinlabs.app",
                note = "SENGKODE test",
            ),
        )
        assertTrue(payload.startsWith("BEGIN:VCARD\n"))
        assertTrue(payload.contains("VERSION:3.0"))
        assertTrue(payload.contains("FN:Jeyhun Mammadov"))
        assertTrue(payload.contains("TEL:+994 50 123 45 67"))
        assertTrue(payload.contains("EMAIL:jeyhun@hjinlabs.app"))
        assertTrue(payload.contains("ORG:HJIN Labs"))
        assertTrue(payload.contains("URL:https://hjinlabs.app"))
        assertTrue(payload.endsWith("END:VCARD\n"))
    }

    @Test
    fun `vcard escapes separators and newlines`() {
        val payload = VCardEncoder.payload(
            QrContent.VCard(
                fullName = "A;B,C",
                organization = null,
                phone = null,
                email = null,
                website = null,
                note = "line1\nline2",
            ),
        )
        assertTrue(payload.contains("FN:A\\;B\\,C"))
        assertTrue(payload.contains("NOTE:line1\\nline2"))
    }

    @Test
    fun `vcard requires a name`() {
        assertTrue(
            VCardEncoder.validate(
                QrContent.VCard("", null, null, null, null, null),
            ) is ValidationResult.Invalid,
        )
    }

    // ---- Email -----------------------------------------------------

    @Test
    fun `mailto payload with subject and body`() {
        assertEquals(
            "mailto:hi@example.com?subject=Hello%20there&body=How%20are%20you%3F",
            EmailEncoder.payload(
                QrContent.Email("hi@example.com", "Hello there", "How are you?"),
            ),
        )
    }

    @Test
    fun `invalid email is rejected`() {
        assertTrue(
            EmailEncoder.validate(QrContent.Email("not-an-email", null, null))
                is ValidationResult.Invalid,
        )
    }

    // ---- SMS / Phone -----------------------------------------------

    @Test
    fun `sms payload format`() {
        assertEquals(
            "SMSTO:+15551234567:Hello!",
            SmsEncoder.payload(QrContent.Sms("+15551234567", "Hello!")),
        )
        assertEquals(
            "SMSTO:+15551234567:",
            SmsEncoder.payload(QrContent.Sms("+1 (555) 123-4567", null)),
        )
    }

    @Test
    fun `phone payload format`() {
        assertEquals("TEL:+15551234567", PhoneEncoder.payload(QrContent.Phone("+15551234567")))
    }

    @Test
    fun `phone without digits is invalid`() {
        assertTrue(
            PhoneEncoder.validate(QrContent.Phone("+++---")) is ValidationResult.Invalid,
        )
    }

    // ---- Geo -------------------------------------------------------

    @Test
    fun `geo payload trims decimals`() {
        assertEquals(
            "GEO:1.3521,103.8198",
            GeoEncoder.payload(QrContent.Geo(1.352100, 103.819800)),
        )
    }

    @Test
    fun `geo out of range is invalid`() {
        assertTrue(
            GeoEncoder.validate(QrContent.Geo(91.0, 0.0)) is ValidationResult.Invalid,
        )
        assertTrue(
            GeoEncoder.validate(QrContent.Geo(0.0, 181.0)) is ValidationResult.Invalid,
        )
    }
}
