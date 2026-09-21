package com.hjinlabs.sengkode.feature.generator.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.style.LogoImage
import com.hjinlabs.sengkode.feature.generator.R
import com.hjinlabs.sengkode.feature.generator.StudioState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_SIZE_PX = 512

/**
 * Renders the live QR preview from [StudioState].
 * Preview == export: the SAME backend renders the SAME ops, off
 * the main thread; recomposition only happens on a new bitmap.
 */
@Composable
internal fun QrPreview(
    state: StudioState,
    renderer: DrawListBitmapRenderer,
    logoImage: LogoImage?,
) {
    val matrix = state.matrix
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
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.large,
            ),
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
                            .widthIn(max = 300.dp)
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
