package com.hjinlabs.sengkode.core.style

import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrStyle

/**
 * Curated, scan-safe presets. A small number of high-quality
 * combinations instead of a large number of untested ones - every
 * preset is validated Safe by the policy (see StylePresetsTest) and
 * by the styled round-trip suite.
 */
data class ColorPreset(
    val name: String,
    val foregroundArgb: Long,
    val backgroundArgb: Long,
)

object StylePresets {

    val COLOR_PRESETS: List<ColorPreset> = listOf(
        ColorPreset("Classic", 0xFF000000L, 0xFFFFFFFFL),
        ColorPreset("Teal", 0xFF00695CL, 0xFFFFFFFFL),
        ColorPreset("Indigo", 0xFF303F9FL, 0xFFFFFFFFL),
        ColorPreset("Crimson", 0xFFB3252BL, 0xFFFFFFFFL),
        ColorPreset("Emerald", 0xFF2E7D32L, 0xFFFFF8E1L),
        ColorPreset("Navy", 0xFF1A237EL, 0xFFECEFF1L),
        ColorPreset("Coffee", 0xFF4E342EL, 0xFFFFF3E0L),
        ColorPreset("Charcoal", 0xFF263238L, 0xFFECEFF1L),
    )

    val MODULE_SHAPES = ModuleShape.entries
    val EYE_SHAPES = EyeShape.entries
}
