package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.FrameSpec
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrContentType
import com.hjinlabs.sengkode.core.model.QrStyle

/**
 * The built-in template gallery. Templates provide QrStyle
 * configuration ONLY - never generation logic: applying a template
 * means seeding the studio's style, then the existing pipeline
 * (engine -> policy -> renderer) does all the work.
 * Every template is decode-verified through the styled round-trip
 * suite (its color pair is a shipped preset; its shape combination
 * is part of the 6-combo sweep).
 */
data class BuiltInTemplate(
    val name: String,
    val description: String,
    val contentTypeHint: QrContentType,
    val style: QrStyle,
)

object BuiltInTemplates {

    val ALL: List<BuiltInTemplate> = listOf(
        BuiltInTemplate(
            name = "Minimal",
            description = "Classic high-contrast squares. Maximum scanner compatibility.",
            contentTypeHint = QrContentType.TEXT,
            style = QrStyle(),
        ),
        BuiltInTemplate(
            name = "Business",
            description = "Indigo, softly rounded modules for cards and print.",
            contentTypeHint = QrContentType.VCARD,
            style = QrStyle(
                foregroundArgb = 0xFF303F9FL,
                backgroundArgb = 0xFFFFFFFFL,
                moduleShape = ModuleShape.ROUNDED,
                eyeShape = EyeShape.ROUNDED,
            ),
        ),
        BuiltInTemplate(
            name = "Social",
            description = "Teal dots - friendly codes for profiles and shares.",
            contentTypeHint = QrContentType.URL,
            style = QrStyle(
                foregroundArgb = 0xFF00695CL,
                backgroundArgb = 0xFFFFFFFFL,
                moduleShape = ModuleShape.DOT,
            ),
        ),
        BuiltInTemplate(
            name = "Event",
            description = "Crimson with a SCAN ME frame for posters and tickets.",
            contentTypeHint = QrContentType.URL,
            style = QrStyle(
                foregroundArgb = 0xFFB3252BL,
                backgroundArgb = 0xFFFFFFFFL,
                moduleShape = ModuleShape.ROUNDED,
                eyeShape = EyeShape.ROUNDED,
                frame = FrameSpec(),
            ),
        ),
        BuiltInTemplate(
            name = "Modern",
            description = "Charcoal on cool gray - quiet, contemporary look.",
            contentTypeHint = QrContentType.TEXT,
            style = QrStyle(
                foregroundArgb = 0xFF263238L,
                backgroundArgb = 0xFFECEFF1L,
                moduleShape = ModuleShape.ROUNDED,
                eyeShape = EyeShape.ROUNDED,
            ),
        ),
    )
}
