package com.hjinlabs.sengkode.core.qr

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.ValidationBuilder
import com.hjinlabs.sengkode.core.model.ValidationResult

/**
 * Encodes one content type into the QR payload string. Each encoder is
 * independent, pure and stateless; validation happens BEFORE generation
 * so invalid content never reaches the writer.
 */
interface QrEncoder<T : QrContent> {

    /** Semantic validation; accumulates every problem, never throws. */
    fun validate(content: T): ValidationResult

    /**
     * Builds the payload string. Only called on validated content;
     * encoders may normalize (e.g. URL scheme prefixing).
     */
    fun payload(content: T): String

    /** Convenience: validate, then build the payload on success. */
    fun encode(content: T): EncoderOutcome {
        val validation = validate(content)
        if (validation is ValidationResult.Invalid) {
            return EncoderOutcome.Invalid(validation.errors)
        }
        return EncoderOutcome.Payload(payload(content))
    }
}

/** Result of a single encoder's encode() call. */
sealed interface EncoderOutcome {
    data class Payload(val value: String) : EncoderOutcome
    data class Invalid(val errors: List<com.hjinlabs.sengkode.core.model.QrError.Validation>) :
        EncoderOutcome
}

/** Shared helpers for all encoders (spec-driven, unit-tested). */
internal object PayloadRules {

    /** Wi-Fi WIFI: escaping per the de-facto spec: escape \ ; , : " */
    fun escapeWifi(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                '\\', ';', ',', ':', '"' -> {
                    append('\\')
                    append(ch)
                }
                else -> append(ch)
            }
        }
    }

    /** vCard text escaping: backslash, semicolon, comma, newline. */
    fun escapeVCard(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                ';' -> append("\\;")
                ',' -> append("\\,")
                '\n' -> append("\\n")
                else -> append(ch)
            }
        }
    }

    /**
     * Normalizes a phone number for machine-readable payloads (SMSTO:,
     * TEL:): keeps digits and the network characters + * #, drops all
     * visual separators (spaces, dashes, parentheses, dots, slashes).
     * Contact-card payloads (vCard TEL) intentionally keep formatting.
     */
    fun normalizePhone(value: String): String =
        value.trim().filter { it.isDigit() || it == '+' || it == '*' || it == '#' }

    fun requireNonBlank(builder: ValidationBuilder, value: String, message: String) {
        builder.require(value.isNotBlank(), message)
    }

    /** Phone-ish validation: at least one digit after trimming noise. */
    fun isValidPhoneNumber(value: String): Boolean {
        val candidate = value.trim()
        if (candidate.isEmpty()) return false
        return candidate.any { it.isDigit() } &&
            candidate.all { it.isDigit() || it in "+-() ./" }
    }

    fun isValidEmail(value: String): Boolean =
        value.isNotBlank() && value.count { it == '@' } == 1 &&
            value.indexOf('@').let { it > 0 && it < value.length - 1 }
}
