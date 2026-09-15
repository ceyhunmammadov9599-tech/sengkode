package com.hjinlabs.sengkode.core.qr

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentType
import com.hjinlabs.sengkode.core.model.ValidationBuilder
import com.hjinlabs.sengkode.core.model.ValidationResult
import com.hjinlabs.sengkode.core.model.WifiEncryption
import com.hjinlabs.sengkode.core.model.type
import java.net.URLEncoder

internal object TextEncoder : QrEncoder<QrContent.Text> {
    override fun validate(content: QrContent.Text): ValidationResult {
        val b = ValidationBuilder()
        PayloadRules.requireNonBlank(b, content.text, "Enter some text to encode.")
        return b.build()
    }

    override fun payload(content: QrContent.Text): String = content.text
}

internal object UrlEncoder : QrEncoder<QrContent.Url> {
    override fun validate(content: QrContent.Url): ValidationResult {
        val b = ValidationBuilder()
        PayloadRules.requireNonBlank(b, content.url, "Enter a web address.")
        return b.build()
    }

    override fun payload(content: QrContent.Url): String {
        val raw = content.url.trim()
        return if (raw.startsWith("http://") || raw.startsWith("https://")) raw
        else "https://$raw"
    }
}

internal object WifiEncoder : QrEncoder<QrContent.Wifi> {
    override fun validate(content: QrContent.Wifi): ValidationResult {
        val b = ValidationBuilder()
        PayloadRules.requireNonBlank(b, content.ssid, "Enter the network name (SSID).")
        when (content.encryption) {
            WifiEncryption.WPA -> {
                if (content.password.length !in 8..63) {
                    b.require(
                        false,
                        "WPA password must be 8 to 63 characters (got ${content.password.length}).",
                    )
                }
            }
            WifiEncryption.WEP -> {
                val ok = content.password.length == 5 || content.password.length == 13
                b.require(ok, "WEP key must be 5 or 13 characters (got ${content.password.length}).")
            }
            WifiEncryption.NOPASS -> {
                b.require(
                    content.password.isEmpty(),
                    "Open networks cannot carry a password - clear the password field.",
                )
            }
        }
        return b.build()
    }

    override fun payload(content: QrContent.Wifi): String = buildString {
        append("WIFI:")
        when (content.encryption) {
            WifiEncryption.WPA -> append("T:WPA;")
            WifiEncryption.WEP -> append("T:WEP;")
            WifiEncryption.NOPASS -> append("T:nopass;")
        }
        append("S:").append(PayloadRules.escapeWifi(content.ssid)).append(';')
        if (content.encryption != WifiEncryption.NOPASS) {
            append("P:").append(PayloadRules.escapeWifi(content.password)).append(';')
        }
        if (content.hidden) append("H:true;")
        append(';')
    }
}

internal object VCardEncoder : QrEncoder<QrContent.VCard> {
    override fun validate(content: QrContent.VCard): ValidationResult {
        val b = ValidationBuilder()
        PayloadRules.requireNonBlank(b, content.fullName, "Enter the contact name.")
        content.email?.let {
            b.require(
                PayloadRules.isValidEmail(it),
                "Enter a valid email address or leave it empty.",
            )
        }
        content.phone?.let {
            b.require(
                PayloadRules.isValidPhoneNumber(it),
                "Enter a valid phone number or leave it empty.",
            )
        }
        return b.build()
    }

    override fun payload(content: QrContent.VCard): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        append("N:").append(PayloadRules.escapeVCard(content.fullName)).appendLine(";")
        append("FN:").append(PayloadRules.escapeVCard(content.fullName)).appendLine()
        content.organization?.let {
            if (it.isNotBlank()) {
                append("ORG:").append(PayloadRules.escapeVCard(it)).appendLine()
            }
        }
        content.phone?.let {
            if (it.isNotBlank()) {
                append("TEL:").append(PayloadRules.escapeVCard(it)).appendLine()
            }
        }
        content.email?.let {
            if (it.isNotBlank()) {
                append("EMAIL:").append(PayloadRules.escapeVCard(it)).appendLine()
            }
        }
        content.website?.let {
            if (it.isNotBlank()) {
                append("URL:").append(PayloadRules.escapeVCard(it)).appendLine()
            }
        }
        content.note?.let {
            if (it.isNotBlank()) {
                append("NOTE:").append(PayloadRules.escapeVCard(it)).appendLine()
            }
        }
        appendLine("END:VCARD")
    }
}

internal object EmailEncoder : QrEncoder<QrContent.Email> {
    override fun validate(content: QrContent.Email): ValidationResult {
        val b = ValidationBuilder()
        b.require(
            PayloadRules.isValidEmail(content.address),
            "Enter a valid email address.",
        )
        return b.build()
    }

    override fun payload(content: QrContent.Email): String = buildString {
        append("mailto:")
        append(content.address.trim())
        val params = mutableListOf<String>()
        content.subject?.takeIf { it.isNotBlank() }?.let {
            params += "subject=${urlEncode(it)}"
        }
        content.body?.takeIf { it.isNotBlank() }?.let {
            params += "body=${urlEncode(it)}"
        }
        if (params.isNotEmpty()) {
            append('?')
            append(params.joinToString("&"))
        }
    }

    /**
     * mailto: is a URI, not an HTML form - spaces must be %20, not the
     * form-encoding '+' that URLEncoder produces (caught by the
     * encoder unit tests, fixed here on purpose).
     */
    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}

internal object SmsEncoder : QrEncoder<QrContent.Sms> {
    override fun validate(content: QrContent.Sms): ValidationResult {
        val b = ValidationBuilder()
        b.require(
            PayloadRules.isValidPhoneNumber(content.number),
            "Enter a valid phone number.",
        )
        return b.build()
    }

    override fun payload(content: QrContent.Sms): String {
        val number = PayloadRules.normalizePhone(content.number)
        val message = content.message?.takeIf { it.isNotBlank() }
        return if (message == null) "SMSTO:$number:" else "SMSTO:$number:$message"
    }
}

internal object PhoneEncoder : QrEncoder<QrContent.Phone> {
    override fun validate(content: QrContent.Phone): ValidationResult {
        val b = ValidationBuilder()
        b.require(
            PayloadRules.isValidPhoneNumber(content.number),
            "Enter a valid phone number.",
        )
        return b.build()
    }

    override fun payload(content: QrContent.Phone): String =
        "TEL:${PayloadRules.normalizePhone(content.number)}"
}

internal object GeoEncoder : QrEncoder<QrContent.Geo> {
    override fun validate(content: QrContent.Geo): ValidationResult {
        val b = ValidationBuilder()
        b.require(
            content.latitude in -90.0..90.0,
            "Latitude must be between -90 and 90.",
        )
        b.require(
            content.longitude in -180.0..180.0,
            "Longitude must be between -180 and 180.",
        )
        return b.build()
    }

    override fun payload(content: QrContent.Geo): String {
        val lat = trimNumber(content.latitude)
        val lon = trimNumber(content.longitude)
        return "GEO:$lat,$lon"
    }

    private fun trimNumber(value: Double): String =
        String.format(java.util.Locale.US, "%.6f", value).trimEnd('0').trimEnd('.')
}
