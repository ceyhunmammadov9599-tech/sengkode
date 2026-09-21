package com.hjinlabs.sengkode.feature.generator

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.hjinlabs.sengkode.core.export.BitmapScannabilityVerifier
import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.style.LogoImage
import com.hjinlabs.sengkode.feature.generator.components.ContentEditor
import com.hjinlabs.sengkode.feature.generator.components.QrPreview
import com.hjinlabs.sengkode.feature.generator.components.TypePicker
import com.hjinlabs.sengkode.feature.generator.components.defaultContentFor
import com.hjinlabs.sengkode.feature.generator.components.export
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The QR studio: content editors at the top, live preview below,
 * export actions at the bottom. The screen renders ONLY from
 * [StudioState]; every change flows through the ViewModel (UDF).
 *
 * GeneratorScreen is the composition root. All sub-sections are
 * extracted into the components/ package for maintainability.
 */
@Composable
fun GeneratorScreen(
    viewModel: StudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val renderer = remember { DrawListBitmapRenderer() }

    // Session handoffs (history regenerate, template apply) land here:
    // consume exactly once on entering composition.
    LaunchedEffect(Unit) { viewModel.consumeRestore() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                StudioEvent.SavedToHistory ->
                    Toast.makeText(context, R.string.saved_to_history, Toast.LENGTH_SHORT).show()
                StudioEvent.SavedAsTemplate ->
                    Toast.makeText(context, R.string.saved_as_template, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showTemplateDialog by remember { mutableStateOf(false) }

    // Logo pixels: a UI asset picked with the system photo picker
    // (ACTION_OPEN_DOCUMENT — no storage permission, privacy intact).
    var logoImage by remember { mutableStateOf<LogoImage?>(null) }

    // Phase 4 runtime scannability gate: when a logo is actually
    // placed, decode the rendered result and require the EXACT
    // payload — the on-device round-trip. null = not applicable.
    var logoVerified by remember { mutableStateOf<Boolean?>(null) }
    val logoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                logoImage = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    }?.let { decodeLogoImage(it) }
                }
            }
        }
    }

    LaunchedEffect(state.matrix, state.style, logoImage) {
        val matrix = state.matrix
        val success = state.result as? QrGenerationResult.Success
        if (matrix == null || success == null || logoImage == null) {
            logoVerified = null
            return@LaunchedEffect
        }
        logoVerified = withContext(Dispatchers.Default) {
            val bitmap = renderer.render(matrix, state.style, 512, logoImage)
            try {
                BitmapScannabilityVerifier.isScannable(bitmap, success.payload)
            } finally {
                bitmap.recycle()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- Preview ----
        QrPreview(state = state, renderer = renderer, logoImage = logoImage)

        if (logoImage != null && logoVerified != null) {
            Text(
                text = stringResource(
                    if (logoVerified == true) R.string.logo_verified
                    else R.string.logo_unverified,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = if (logoVerified == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        // ---- Content ----
        TypePicker(selected = state.content.type, onSelected = { type ->
            viewModel.updateContent(defaultContentFor(type))
        })

        ContentEditor(content = state.content, onContentChange = viewModel::updateContent)

        // ---- ECC ----
        EccPicker(selected = state.ecc, onSelected = viewModel::updateEcc)
        if (state.effectiveEcc != state.ecc) {
            Text(
                text = stringResource(R.string.ecc_effective, state.effectiveEcc.name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        // ---- Style ----
        StyleSections(
            style = state.style,
            onStyleChange = { newStyle -> viewModel.setStyle(newStyle) },
            onPickLogo = { logoPicker.launch(arrayOf("image/*")) },
            onRemoveLogoImage = { logoImage = null },
            logoPicked = logoImage != null,
        )

        // ---- Export actions ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    scope.launch {
                        export(state, context, renderer, logoImage, logoVerified, share = false)
                    }
                },
                enabled = state.canRender,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_save))
            }
            androidx.compose.material3.FilledTonalButton(
                onClick = {
                    scope.launch {
                        export(state, context, renderer, logoImage, logoVerified, share = true)
                    }
                },
                enabled = state.canRender,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_share))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = { viewModel.saveToHistory() },
                enabled = state.matrix != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_save_history))
            }
            androidx.compose.material3.OutlinedButton(
                onClick = { showTemplateDialog = true },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_save_template))
            }
        }

        if (showTemplateDialog) {
            var templateName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showTemplateDialog = false },
                title = { Text(stringResource(R.string.action_save_template)) },
                text = {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = { templateName = it },
                        label = { Text(stringResource(R.string.template_name_hint)) },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showTemplateDialog = false
                            viewModel.saveAsTemplate(templateName)
                        },
                    ) {
                        Text(stringResource(R.string.template_save_cta))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTemplateDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
    }
}

/**
 * ECC picker — segmented button row.
 * Kept in this file as it is tightly coupled to the screen's ECC
 * state display (effective ECC label) and is not reused elsewhere.
 */
@Composable
private fun EccPicker(selected: EccLevel, onSelected: (EccLevel) -> Unit) {
    Text(
        text = stringResource(R.string.ecc_label),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        EccLevel.entries.forEachIndexed { index, level ->
            SegmentedButton(
                selected = level == selected,
                onClick = { onSelected(level) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = EccLevel.entries.size,
                ),
            ) {
                Text(level.name)
            }
        }
    }
}

/**
 * Downsamples a picked logo to bounded pixels (<= 256px side): the
 * render-side blit scales again, so huge inputs only cost memory.
 */
private fun decodeLogoImage(bitmap: android.graphics.Bitmap): LogoImage {
    val maxSide = 256
    val scaled = if (maxOf(bitmap.width, bitmap.height) > maxSide) {
        val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
        android.graphics.Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        bitmap
    }
    val pixels = IntArray(scaled.width * scaled.height)
    scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
    return LogoImage(pixels, scaled.width, scaled.height)
}
