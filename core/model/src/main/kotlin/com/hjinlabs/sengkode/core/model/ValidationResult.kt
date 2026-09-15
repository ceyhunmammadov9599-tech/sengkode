package com.hjinlabs.sengkode.core.model

/**
 * Result of pre-generation content validation. Invalid carries every
 * problem found (the UI shows them all at once, not one at a time).
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: List<QrError.Validation>) : ValidationResult {
        init {
            require(errors.isNotEmpty()) { "Invalid must carry at least one error" }
        }
    }

    val isValid: Boolean
        get() = this is Valid
}

/** Collects validation problems with a fluent, immutable API. */
class ValidationBuilder {
    private val problems = mutableListOf<QrError.Validation>()

    fun require(condition: Boolean, message: String) {
        if (!condition) problems += QrError.Validation(message)
    }

    fun build(): ValidationResult =
        if (problems.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(problems.toList())
}
