package com.hjinlabs.sengkode.feature.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.LogoSpec
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.style.StylePresets

/**
 * Customization sections. Every control writes through a single
 * updateStyle(transform) entry point - sections never touch colors
 * or safety logic themselves (the policy owns that).
 */

@Composable
internal fun StyleSections(
    style: QrStyle,
    onStyleChange: (QrStyle) -> Unit,
    onPickLogo: () -> Unit,
    logoPicked: Boolean,
) {
    SectionCard(stringResource(R.string.section_colors)) {
        ColorSection(style, onStyleChange)
    }
    SectionCard(stringResource(R.string.section_shape)) {
        ShapeSection(style, onStyleChange)
    }
    SectionCard(stringResource(R.string.section_eyes)) {
        EyeSection(style, onStyleChange)
    }
    SectionCard(stringResource(R.string.section_logo)) {
        LogoSection(style, onStyleChange, onPickLogo, logoPicked)
    }
    SectionCard(stringResource(R.string.section_frame)) {
        FrameSection(style, onStyleChange)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

// ---------------------------------------------------------------------
// Colors
// ---------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSection(style: QrStyle, onStyleChange: (QrStyle) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StylePresets.COLOR_PRESETS.forEach { preset ->
            val label = stringResource(R.string.cd_color_preset, preset.name)
            FilterChip(
                selected = style.foregroundArgb == preset.foregroundArgb &&
                    style.backgroundArgb == preset.backgroundArgb,
                onClick = {
                    onStyleChange(
                        style.copy(
                            foregroundArgb = preset.foregroundArgb,
                            backgroundArgb = preset.backgroundArgb,
                        ),
                    )
                },
                label = { Text(preset.name) },
                modifier = Modifier.semantics { contentDescription = label },
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HexField(
            label = stringResource(R.string.color_custom_fg),
            value = style.foregroundArgb,
            onParsed = { onStyleChange(style.copy(foregroundArgb = it)) },
            modifier = Modifier.weight(1f),
        )
        HexField(
            label = stringResource(R.string.color_custom_bg),
            value = style.backgroundArgb,
            onParsed = { onStyleChange(style.copy(backgroundArgb = it)) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HexField(
    label: String,
    value: Long,
    onParsed: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by androidx.compose.runtime.remember(value) {
        androidx.compose.runtime.mutableStateOf(colorToHex(value))
    }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input
            val parsed = hexToColorOrNull(input)
            if (parsed != null) onParsed(parsed)
        },
        label = { Text(label) },
        singleLine = true,
        isError = text.isNotBlank() && hexToColorOrNull(text) == null,
        supportingText = {
            if (text.isNotBlank() && hexToColorOrNull(text) == null) {
                Text(stringResource(R.string.color_invalid_hex))
            }
        },
        modifier = modifier,
    )
}

internal fun colorToHex(argb: Long): String =
    "%06X".format(argb and 0xFFFFFFL)

internal fun hexToColorOrNull(hex: String): Long? {
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    val value = cleaned.toLongOrNull(16) ?: return null
    return 0xFF000000L or value
}

// ---------------------------------------------------------------------
// Modules / Eyes
// ---------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapeSection(style: QrStyle, onStyleChange: (QrStyle) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModuleShape.entries.forEach { shape ->
            FilterChip(
                selected = style.moduleShape == shape,
                onClick = { onStyleChange(style.copy(moduleShape = shape)) },
                label = { Text(shape.name.lowercase().replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EyeSection(style: QrStyle, onStyleChange: (QrStyle) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EyeShape.entries.forEach { shape ->
            FilterChip(
                selected = style.eyeShape == shape,
                onClick = { onStyleChange(style.copy(eyeShape = shape)) },
                label = { Text(shape.name.lowercase().replaceFirstChar { it.uppercase() }) },
            )
        }
        FilterChip(
            selected = style.eyeColorArgb == null,
            onClick = { onStyleChange(style.copy(eyeColorArgb = null)) },
            label = { Text(stringResource(R.string.eye_color_same)) },
        )
    }
    if (style.eyeColorArgb != null) {
        var text by androidx.compose.runtime.remember(style.eyeColorArgb) {
            androidx.compose.runtime.mutableStateOf(colorToHex(style.eyeColorArgb!!))
        }
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                hexToColorOrNull(input)?.let { onStyleChange(style.copy(eyeColorArgb = it)) }
            },
            label = { Text(stringResource(R.string.eye_color_custom)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------------------------------------------------------------
// Logo
// ---------------------------------------------------------------------

@Composable
private fun LogoSection(
    style: QrStyle,
    onStyleChange: (QrStyle) -> Unit,
    onPickLogo: () -> Unit,
    logoPicked: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.logo_enable))
        Switch(
            checked = style.logo != null,
            onCheckedChange = { enabled ->
                onStyleChange(style.copy(logo = if (enabled) LogoSpec() else null))
            },
        )
    }
    if (style.logo != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (logoPicked) {
                AssistChip(
                    onClick = onPickLogo,
                    label = { Text(stringResource(R.string.logo_pick)) },
                )
            } else {
                AssistChip(
                    onClick = onPickLogo,
                    label = { Text(stringResource(R.string.logo_pick)) },
                )
            }
        }
        Text(
            text = stringResource(
                R.string.logo_size,
                (style.logo!!.sizeFraction * 100).toInt(),
            ),
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = style.logo!!.sizeFraction,
            onValueChange = { fraction ->
                onStyleChange(
                    style.copy(logo = LogoSpec(sizeFraction = fraction.coerceIn(0.10f, 0.25f))),
                )
            },
            valueRange = LogoSpec.MIN_FRACTION..LogoSpec.MAX_FRACTION,
        )
    }
}

// ---------------------------------------------------------------------
// Frame
// ---------------------------------------------------------------------

@Composable
private fun FrameSection(style: QrStyle, onStyleChange: (QrStyle) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.frame_enable))
        Switch(
            checked = style.frame != null,
            onCheckedChange = { enabled ->
                onStyleChange(
                    style.copy(
                        frame = if (enabled) com.hjinlabs.sengkode.core.model.FrameSpec() else null,
                    ),
                )
            },
        )
    }
}
