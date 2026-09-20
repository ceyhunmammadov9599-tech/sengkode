package com.hjinlabs.sengkode.feature.generator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.hjinlabs.sengkode.core.model.EyeShape
import com.hjinlabs.sengkode.core.model.LogoSpec
import com.hjinlabs.sengkode.core.model.ModuleShape
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.style.StylePresets

/**
 * Customization area (Phase 2 UX modernization). All five former
 * always-visible cards now live inside ONE collapsed-by-default
 * "Customize appearance" section, each as a compact expandable row.
 * Every control still writes through the single
 * updateStyle(transform) entry point - sections never touch colors
 * or safety logic themselves (the policy owns that). All styling
 * behavior, presets, hex parsing, logo and frame logic are
 * unchanged; only the presentation is new.
 */

@Composable
internal fun StyleSections(
    style: QrStyle,
    onStyleChange: (QrStyle) -> Unit,
    onPickLogo: () -> Unit,
    onRemoveLogoImage: () -> Unit,
    logoPicked: Boolean,
) {
    var customizeOpen by rememberSaveable { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExpandableHeader(
                title = stringResource(R.string.section_customize),
                expanded = customizeOpen,
                onToggle = { customizeOpen = !customizeOpen },
            )
            AnimatedVisibility(visible = customizeOpen) {
                Column {
                    ExpandableRow(stringResource(R.string.section_colors)) {
                        ColorSection(style, onStyleChange)
                    }
                    ExpandableRow(stringResource(R.string.section_shape)) {
                        ShapeSection(style, onStyleChange)
                    }
                    ExpandableRow(stringResource(R.string.section_eyes)) {
                        EyeSection(style, onStyleChange)
                    }
                    ExpandableRow(stringResource(R.string.section_logo)) {
                        LogoSection(style, onStyleChange, onPickLogo, onRemoveLogoImage, logoPicked)
                    }
                    ExpandableRow(stringResource(R.string.section_frame)) {
                        FrameSection(style, onStyleChange)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Shared expandable building blocks
// ---------------------------------------------------------------------

@Composable
private fun ExpandableHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val expandedText = stringResource(R.string.state_expanded)
    val collapsedText = stringResource(R.string.state_collapsed)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .minimumInteractiveComponentSize()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = if (expanded) expandedText else collapsedText
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExpandableRow(
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val expandedText = stringResource(R.string.state_expanded)
    val collapsedText = stringResource(R.string.state_collapsed)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = !expanded })
                .minimumInteractiveComponentSize()
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    stateDescription = if (expanded) expandedText else collapsedText
                }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

// ---------------------------------------------------------------------
// Colors: visual swatches first, hex behind a dialog
// ---------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSection(style: QrStyle, onStyleChange: (QrStyle) -> Unit) {
    val swatchDescription = stringResource(R.string.cd_color_preset)
    val selectedText = stringResource(R.string.state_selected)
    val unselectedText = stringResource(R.string.state_unselected)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StylePresets.COLOR_PRESETS.forEach { preset ->
            val selected = style.foregroundArgb == preset.foregroundArgb &&
                style.backgroundArgb == preset.backgroundArgb
            val label = "$swatchDescription ${preset.name}"
            val borderColor = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
            val stateText = if (selected) selectedText else unselectedText
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .minimumInteractiveComponentSize()
                    .semantics(mergeDescendants = true) {
                        contentDescription = label
                        stateDescription = stateText
                    }
                    .border(2.dp, borderColor, CircleShape)
                    .padding(4.dp)
                    .background(Color(preset.backgroundArgb.toInt()), CircleShape)
                    .clickable {
                        onStyleChange(
                            style.copy(
                                foregroundArgb = preset.foregroundArgb,
                                backgroundArgb = preset.backgroundArgb,
                            ),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color(preset.foregroundArgb.toInt()),
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                Color(preset.foregroundArgb.toInt()),
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }

    var showHexDialog by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = { showHexDialog = true },
            label = { Text(stringResource(R.string.color_custom_open)) },
        )
    }
    if (showHexDialog) {
        AlertDialog(
            onDismissRequest = { showHexDialog = false },
            confirmButton = {
                TextButton(onClick = { showHexDialog = false }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            title = { Text(stringResource(R.string.color_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HexField(
                        label = stringResource(R.string.color_custom_fg),
                        value = style.foregroundArgb,
                        onParsed = { onStyleChange(style.copy(foregroundArgb = it)) },
                    )
                    HexField(
                        label = stringResource(R.string.color_custom_bg),
                        value = style.backgroundArgb,
                        onParsed = { onStyleChange(style.copy(backgroundArgb = it)) },
                    )
                    Text(
                        text = stringResource(R.string.color_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
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
    var text by remember(value) {
        mutableStateOf(colorToHex(value))
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
// Modules / Eyes: compact segmented selectors
// ---------------------------------------------------------------------

@Composable
private fun ShapeSection(style: QrStyle, onStyleChange: (QrStyle) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ModuleShape.entries.forEachIndexed { index, shape ->
            SegmentedButton(
                selected = style.moduleShape == shape,
                onClick = { onStyleChange(style.copy(moduleShape = shape)) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = ModuleShape.entries.size,
                ),
            ) {
                Text(shape.label())
            }
        }
    }
}

private fun ModuleShape.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun EyeSection(style: QrStyle, onStyleChange: (QrStyle) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        EyeShape.entries.forEachIndexed { index, shape ->
            SegmentedButton(
                selected = style.eyeShape == shape,
                onClick = { onStyleChange(style.copy(eyeShape = shape)) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = EyeShape.entries.size,
                ),
            ) {
                Text(shape.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
    }
    var showEyeDialog by rememberSaveable { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = style.eyeColorArgb == null,
            onClick = {
                showEyeDialog = false
                onStyleChange(style.copy(eyeColorArgb = null))
            },
            label = { Text(stringResource(R.string.eye_color_same)) },
        )
        FilterChip(
            selected = style.eyeColorArgb != null,
            onClick = {
                if (style.eyeColorArgb == null) {
                    onStyleChange(style.copy(eyeColorArgb = style.foregroundArgb))
                }
                showEyeDialog = true
            },
            label = { Text(stringResource(R.string.eye_color_custom)) },
        )
    }
    if (showEyeDialog) {
        AlertDialog(
            onDismissRequest = { showEyeDialog = false },
            confirmButton = {
                TextButton(onClick = { showEyeDialog = false }) {
                    Text(stringResource(R.string.action_done))
                }
            },
            title = { Text(stringResource(R.string.eye_color_custom)) },
            text = {
                HexField(
                    label = stringResource(R.string.eye_color_custom),
                    value = style.eyeColorArgb ?: style.foregroundArgb,
                    onParsed = { onStyleChange(style.copy(eyeColorArgb = it)) },
                )
            },
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
    onRemoveLogoImage: () -> Unit,
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
            AssistChip(
                onClick = onPickLogo,
                label = {
                    Text(
                        stringResource(
                            if (logoPicked) R.string.logo_pick_change else R.string.logo_pick,
                        ),
                    )
                },
            )
            if (logoPicked) {
                AssistChip(
                    onClick = {
                        onRemoveLogoImage()
                        onStyleChange(style.copy(logo = null))
                    },
                    label = { Text(stringResource(R.string.logo_remove_image)) },
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
        val sizeLabel = stringResource(R.string.logo_size_cd)
        Slider(
            value = style.logo!!.sizeFraction,
            onValueChange = { fraction ->
                onStyleChange(
                    style.copy(logo = LogoSpec(sizeFraction = fraction.coerceIn(0.10f, 0.25f))),
                )
            },
            valueRange = LogoSpec.MIN_FRACTION..LogoSpec.MAX_FRACTION,
            modifier = Modifier.semantics { contentDescription = sizeLabel },
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
