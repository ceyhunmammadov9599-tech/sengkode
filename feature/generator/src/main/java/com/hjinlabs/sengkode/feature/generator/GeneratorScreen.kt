package com.hjinlabs.sengkode.feature.generator

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.export.QrFileExporter
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.ExportSpec
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.WifiEncryption
import com.hjinlabs.sengkode.core.model.type
import com.hjinlabs.sengkode.core.style.LogoImage
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The QR studio: content editors at the top, live preview below,
 * export actions at the bottom. The screen renders ONLY from
 * [StudioState]; every change flows through the ViewModel (UDF).
 */
@Composable
fun GeneratorScreen(
    viewModel: StudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val renderer = remember { DrawListBitmapRenderer() }

    // Logo pixels: a UI asset picked with the system photo picker
    // (ACTION_OPEN_DOCUMENT - no storage permission, privacy intact).
    var logoImage by remember { mutableStateOf<LogoImage?>(null) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TypePicker(selected = state.content.type, onSelected = { type ->
            viewModel.updateContent(defaultContentFor(type))
        })

        QrPreview(state = state, renderer = renderer, logoImage = logoImage)

        ContentEditor(content = state.content, onContentChange = viewModel::updateContent)

        EccPicker(selected = state.ecc, onSelected = viewModel::updateEcc)
        if (state.effectiveEcc != state.ecc) {
            Text(
                text = stringResource(R.string.ecc_effective, state.effectiveEcc.name),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        StyleSections(
            style = state.style,
            onStyleChange = { newStyle -> viewModel.setStyle(newStyle) },
            onPickLogo = { logoPicker.launch(arrayOf("image/*")) },
            logoPicked = logoImage != null,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    scope.launch {
                        export(state, context, renderer, logoImage, share = false)
                    }
                },
                enabled = state.canRender,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_save))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        export(state, context, renderer, logoImage, share = true)
                    }
                },
                enabled = state.canRender,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_share))
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

// ---------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------

private const val PREVIEW_SIZE_PX = 512

@Composable
private fun QrPreview(
    state: StudioState,
    renderer: DrawListBitmapRenderer,
    logoImage: LogoImage?,
) {
    val matrix = state.matrix
    // Preview == export: the SAME backend renders the SAME ops, off
    // the main thread; recomposition only happens on a new bitmap.
    var preview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(matrix, state.style, logoImage, state.safety) {
        if (matrix == null || state.safety !is com.hjinlabs.sengkode.core.style.ScanSafety.Safe) {
            preview = null
            return@LaunchedEffect
        }
        preview = withContext(Dispatchers.Default) {
            renderer.render(matrix, state.style, PREVIEW_SIZE_PX, logoImage)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val current = preview
            when {
                current != null -> {
                    Image(
                        bitmap = current.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_qr_preview),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .aspectRatio(1f),
                    )
                }
                state.safety is com.hjinlabs.sengkode.core.style.ScanSafety.Unsafe -> {
                    Text(
                        text = stringResource(R.string.preview_unsafe_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    (state.safety as com.hjinlabs.sengkode.core.style.ScanSafety.Unsafe)
                        .reasons.forEach { reason ->
                            Text(
                                text = reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                }
                state.error != null -> {
                    Text(
                        text = state.error!!.userMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.preview_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Pickers
// ---------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypePicker(
    selected: com.hjinlabs.sengkode.core.model.QrContentType,
    onSelected: (com.hjinlabs.sengkode.core.model.QrContentType) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.hjinlabs.sengkode.core.model.QrContentType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelected(type) },
                label = { Text(typeLabel(type)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EccPicker(selected: EccLevel, onSelected: (EccLevel) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.ecc_label),
            style = MaterialTheme.typography.labelLarge,
        )
        EccLevel.entries.forEach { level ->
            FilterChip(
                selected = level == selected,
                onClick = { onSelected(level) },
                label = { Text(level.name) },
            )
        }
    }
}

// ---------------------------------------------------------------------
// Content editors
// ---------------------------------------------------------------------

@Composable
private fun ContentEditor(
    content: QrContent,
    onContentChange: (QrContent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (content) {
            is QrContent.Text -> {
                Field(stringResource(R.string.field_text), content.text, true) {
                    onContentChange(QrContent.Text(it))
                }
            }
            is QrContent.Url -> {
                Field(stringResource(R.string.field_url), content.url, true) {
                    onContentChange(QrContent.Url(it))
                }
            }
            is QrContent.Wifi -> WifiEditor(content, onContentChange)
            is QrContent.VCard -> VCardEditor(content, onContentChange)
            is QrContent.Email -> EmailEditor(content, onContentChange)
            is QrContent.Sms -> SmsEditor(content, onContentChange)
            is QrContent.Phone -> {
                Field(stringResource(R.string.field_phone), content.number, true) {
                    onContentChange(QrContent.Phone(it))
                }
            }
            is QrContent.Geo -> GeoEditor(content, onContentChange)
        }
    }
}

@Composable
private fun WifiEditor(content: QrContent.Wifi, onContentChange: (QrContent) -> Unit) {
    Field(stringResource(R.string.field_wifi_ssid), content.ssid, true) {
        onContentChange(content.copy(ssid = it))
    }
    EncryptionPicker(content.encryption) { encryption ->
        onContentChange(
            content.copy(
                encryption = encryption,
                password = if (encryption == WifiEncryption.NOPASS) "" else content.password,
            ),
        )
    }
    if (content.encryption != WifiEncryption.NOPASS) {
        Field(stringResource(R.string.field_wifi_password), content.password, true) {
            onContentChange(content.copy(password = it))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = content.hidden,
            onClick = { onContentChange(content.copy(hidden = !content.hidden)) },
            label = { Text(stringResource(R.string.field_wifi_hidden)) },
        )
    }
}

@Composable
private fun EncryptionPicker(selected: WifiEncryption, onSelected: (WifiEncryption) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            WifiEncryption.WPA to R.string.encryption_wpa,
            WifiEncryption.WEP to R.string.encryption_wep,
            WifiEncryption.NOPASS to R.string.encryption_nopass,
        ).forEach { (encryption, labelRes) ->
            FilterChip(
                selected = encryption == selected,
                onClick = { onSelected(encryption) },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }
}

@Composable
private fun VCardEditor(content: QrContent.VCard, onContentChange: (QrContent) -> Unit) {
    Field(stringResource(R.string.field_name), content.fullName, true) {
        onContentChange(content.copy(fullName = it))
    }
    Field(stringResource(R.string.field_organization), content.organization.orEmpty()) {
        onContentChange(content.copy(organization = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_phone), content.phone.orEmpty()) {
        onContentChange(content.copy(phone = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_email), content.email.orEmpty()) {
        onContentChange(content.copy(email = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_website), content.website.orEmpty()) {
        onContentChange(content.copy(website = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_note), content.note.orEmpty()) {
        onContentChange(content.copy(note = it.ifBlank { null }))
    }
}

@Composable
private fun EmailEditor(content: QrContent.Email, onContentChange: (QrContent) -> Unit) {
    Field(stringResource(R.string.field_email), content.address, true) {
        onContentChange(content.copy(address = it))
    }
    Field(stringResource(R.string.field_subject), content.subject.orEmpty()) {
        onContentChange(content.copy(subject = it.ifBlank { null }))
    }
    Field(stringResource(R.string.field_body), content.body.orEmpty()) {
        onContentChange(content.copy(body = it.ifBlank { null }))
    }
}

@Composable
private fun SmsEditor(content: QrContent.Sms, onContentChange: (QrContent) -> Unit) {
    Field(stringResource(R.string.field_sms_number), content.number, true) {
        onContentChange(content.copy(number = it))
    }
    Field(stringResource(R.string.field_sms_message), content.message.orEmpty()) {
        onContentChange(content.copy(message = it.ifBlank { null }))
    }
}

@Composable
private fun GeoEditor(content: QrContent.Geo, onContentChange: (QrContent) -> Unit) {
    var latText by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(content.latitude.toString())
    }
    var lonText by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(content.longitude.toString())
    }
    Field(
        label = stringResource(R.string.field_latitude),
        value = latText,
        singleLine = true,
        onValueChange = { latText = it },
    )
    Field(
        label = stringResource(R.string.field_longitude),
        value = lonText,
        singleLine = true,
        onValueChange = { lonText = it },
    )
    androidx.compose.runtime.LaunchedEffect(latText, lonText) {
        val lat = latText.toDoubleOrNull()
        val lon = lonText.toDoubleOrNull()
        if (lat != null && lon != null) {
            onContentChange(QrContent.Geo(lat, lon))
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    singleLine: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun typeLabel(type: com.hjinlabs.sengkode.core.model.QrContentType): String =
    stringResource(
        when (type) {
            com.hjinlabs.sengkode.core.model.QrContentType.TEXT -> R.string.type_text
            com.hjinlabs.sengkode.core.model.QrContentType.URL -> R.string.type_url
            com.hjinlabs.sengkode.core.model.QrContentType.WIFI -> R.string.type_wifi
            com.hjinlabs.sengkode.core.model.QrContentType.VCARD -> R.string.type_vcard
            com.hjinlabs.sengkode.core.model.QrContentType.EMAIL -> R.string.type_email
            com.hjinlabs.sengkode.core.model.QrContentType.SMS -> R.string.type_sms
            com.hjinlabs.sengkode.core.model.QrContentType.PHONE -> R.string.type_phone
            com.hjinlabs.sengkode.core.model.QrContentType.GEO -> R.string.type_geo
        },
    )

private fun defaultContentFor(type: com.hjinlabs.sengkode.core.model.QrContentType): QrContent =
    when (type) {
        com.hjinlabs.sengkode.core.model.QrContentType.TEXT -> QrContent.Text("")
        com.hjinlabs.sengkode.core.model.QrContentType.URL -> QrContent.Url("")
        com.hjinlabs.sengkode.core.model.QrContentType.WIFI ->
            QrContent.Wifi("", "", WifiEncryption.WPA, false)
        com.hjinlabs.sengkode.core.model.QrContentType.VCARD ->
            QrContent.VCard("", null, null, null, null, null)
        com.hjinlabs.sengkode.core.model.QrContentType.EMAIL ->
            QrContent.Email("", null, null)
        com.hjinlabs.sengkode.core.model.QrContentType.SMS -> QrContent.Sms("", null)
        com.hjinlabs.sengkode.core.model.QrContentType.PHONE -> QrContent.Phone("")
        com.hjinlabs.sengkode.core.model.QrContentType.GEO -> QrContent.Geo(1.3521, 103.8198)
    }

// ---------------------------------------------------------------------
// Export actions
// ---------------------------------------------------------------------

private suspend fun export(
    state: StudioState,
    context: android.content.Context,
    renderer: DrawListBitmapRenderer,
    logoImage: LogoImage?,
    share: Boolean,
) {
    val success = state.result as? QrGenerationResult.Success ?: return
    // Unsafe styles are never exported - the same gate the preview uses.
    if (state.safety !is com.hjinlabs.sengkode.core.style.ScanSafety.Safe) return
    val exporter = QrFileExporter(context)
    val spec = ExportSpec(displayName = "sengkode-${System.currentTimeMillis()}")
    val bitmap = withContext(Dispatchers.Default) {
        renderer.render(success.matrix, state.style, spec.sizePx, logoImage)
    }
    if (share) {
        exporter.sharePng(bitmap, spec)
    } else {
        val result = withContext(Dispatchers.IO) {
            exporter.saveToGallery(bitmap, spec)
        }
        val message = when {
            result.isSuccess && android.os.Build.VERSION.SDK_INT >= 29 ->
                context.getString(R.string.saved_to_gallery)
            result.isSuccess -> context.getString(R.string.saved_to_app_storage)
            else -> context.getString(R.string.save_failed)
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
