package com.hjinlabs.sengkode.feature.generator.components

import android.widget.Toast
import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.export.QrFileExporter
import com.hjinlabs.sengkode.core.model.ExportSpec
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.style.LogoImage
import com.hjinlabs.sengkode.feature.generator.R
import com.hjinlabs.sengkode.feature.generator.StudioState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Suspend export helper shared by Save and Share actions.
 * Runs entirely off the main thread except for the final Toast.
 * No UI composables — called from a coroutine scope in GeneratorScreen.
 */
internal suspend fun export(
    state: StudioState,
    context: android.content.Context,
    renderer: DrawListBitmapRenderer,
    logoImage: LogoImage?,
    logoVerified: Boolean?,
    share: Boolean,
) {
    val success = state.result as? QrGenerationResult.Success ?: return
    // Unsafe styles are never exported — same gate as the preview.
    if (state.safety !is com.hjinlabs.sengkode.core.style.ScanSafety.Safe) return
    // Phase 4: a logo that breaks decoding blocks the export too.
    if (logoImage != null && logoVerified == false) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.logo_unverified, Toast.LENGTH_SHORT).show()
        }
        return
    }
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
