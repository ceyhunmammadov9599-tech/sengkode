package com.hjinlabs.sengkode.core.qr

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentType
import com.hjinlabs.sengkode.core.model.type

/**
 * Dispatch table from content type to encoder. Adding a content type
 * in a later phase means: one encoder + one registry entry + tests -
 * nothing else in the engine changes.
 */
class QrEncoderRegistry {

    private val encoders: Map<QrContentType, QrEncoder<*>> = mapOf(
        QrContentType.TEXT to TextEncoder,
        QrContentType.URL to UrlEncoder,
        QrContentType.WIFI to WifiEncoder,
        QrContentType.VCARD to VCardEncoder,
        QrContentType.EMAIL to EmailEncoder,
        QrContentType.SMS to SmsEncoder,
        QrContentType.PHONE to PhoneEncoder,
        QrContentType.GEO to GeoEncoder,
    )

    @Suppress("UNCHECKED_CAST")
    internal fun encoderFor(content: QrContent): QrEncoder<QrContent> {
        val encoder = requireNotNull(encoders[content.type]) {
            "No encoder registered for ${content.type}"
        }
        return encoder as QrEncoder<QrContent>
    }
}
