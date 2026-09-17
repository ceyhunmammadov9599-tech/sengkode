package com.hjinlabs.sengkode.core.model

/**
 * Editor defaults per content type - shared by the studio, history
 * regeneration and template application, so every surface seeds
 * the same empty state.
 */
object QrContentDefaults {

    fun defaultFor(type: QrContentType): QrContent = when (type) {
        QrContentType.TEXT -> QrContent.Text("")
        QrContentType.URL -> QrContent.Url("")
        QrContentType.WIFI -> QrContent.Wifi("", "", WifiEncryption.WPA, false)
        QrContentType.VCARD -> QrContent.VCard("", null, null, null, null, null)
        QrContentType.EMAIL -> QrContent.Email("", null, null)
        QrContentType.SMS -> QrContent.Sms("", null)
        QrContentType.PHONE -> QrContent.Phone("")
        QrContentType.GEO -> QrContent.Geo(0.0, 0.0)
    }

    /** Human title for a snapshot (history rows, notifications). */
    fun titleFor(content: QrContent): String = when (content) {
        is QrContent.Text -> content.text.ifBlank { "Text" }
        is QrContent.Url -> content.url.ifBlank { "URL" }
        is QrContent.Wifi -> "Wi-Fi ${content.ssid}".trim()
        is QrContent.VCard -> content.fullName.ifBlank { "Contact" }
        is QrContent.Email -> content.address.ifBlank { "Email" }
        is QrContent.Sms -> "SMS ${content.number}".trim()
        is QrContent.Phone -> content.number.ifBlank { "Phone" }
        is QrContent.Geo -> "Location"
    }
}
