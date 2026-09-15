package com.hjinlabs.sengkode.core.model

/**
 * Domain content types supported by the v1 engine. Immutable, pure
 * Kotlin, no Android dependencies. Each variant is produced by the UI
 * editors and consumed exclusively by the :core:qr engine.
 */
sealed interface QrContent {

    /** Plain free text. */
    data class Text(val text: String) : QrContent

    /** Web URL; the encoder normalizes missing schemes. */
    data class Url(val url: String) : QrContent

    /**
     * Wi-Fi join configuration per the WIFI: de-facto specification.
     * [password] must be empty for [WifiEncryption.NOPASS].
     */
    data class Wifi(
        val ssid: String,
        val password: String,
        val encryption: WifiEncryption,
        val hidden: Boolean,
    ) : QrContent

    /** Contact card (vCard 3.0). [fullName] is required. */
    data class VCard(
        val fullName: String,
        val organization: String?,
        val phone: String?,
        val email: String?,
        val website: String?,
        val note: String?,
    ) : QrContent

    /** Email with optional subject and body. */
    data class Email(
        val address: String,
        val subject: String?,
        val body: String?,
    ) : QrContent

    /** SMS with optional pre-filled message. */
    data class Sms(val number: String, val message: String?) : QrContent

    /** Phone number for a dial action. */
    data class Phone(val number: String) : QrContent

    /** Geographic coordinates. */
    data class Geo(val latitude: Double, val longitude: Double) : QrContent
}

enum class WifiEncryption { WPA, WEP, NOPASS }

enum class QrContentType {
    TEXT, URL, WIFI, VCARD, EMAIL, SMS, PHONE, GEO
}

/** Maps content to its type tag (single source of truth for dispatch). */
val QrContent.type: QrContentType
    get() = when (this) {
        is QrContent.Text -> QrContentType.TEXT
        is QrContent.Url -> QrContentType.URL
        is QrContent.Wifi -> QrContentType.WIFI
        is QrContent.VCard -> QrContentType.VCARD
        is QrContent.Email -> QrContentType.EMAIL
        is QrContent.Sms -> QrContentType.SMS
        is QrContent.Phone -> QrContentType.PHONE
        is QrContent.Geo -> QrContentType.GEO
    }
