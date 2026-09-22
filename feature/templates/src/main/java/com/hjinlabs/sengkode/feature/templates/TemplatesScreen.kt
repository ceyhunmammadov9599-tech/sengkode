package com.hjinlabs.sengkode.feature.templates

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentDefaults
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.qr.QrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Template gallery: each card previews the template style on a
 * sample code rendered through the SAME pipeline as the studio
 * (engine -> renderer) - what you see is exactly what applying
 * produces.
 */
@Composable
fun TemplatesScreen(
    onApply: (Pair<QrContent, com.hjinlabs.sengkode.core.model.QrStyle>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text(stringResource(R.string.templates_title)) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(templates, key = { it.id }) { template ->
                TemplateCard(
                    name = template.name,
                    description = template.description,
                    style = template.style,
                    isBuiltIn = template.isBuiltIn,
                    engine = viewModel.engine,
                    renderer = viewModel.renderer,
                    onApply = { onApply(viewModel.apply(template)) },
                    onDelete = if (template.isBuiltIn) null else { { viewModel.deleteCustom(template) } },
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(
    name: String,
    description: String,
    style: com.hjinlabs.sengkode.core.model.QrStyle,
    isBuiltIn: Boolean,
    engine: QrEngine,
    renderer: DrawListBitmapRenderer,
    onApply: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    var preview by remember(style) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(style) {
        preview = withContext(Dispatchers.Default) {
            val content = QrContent.Text("SENGKODE $name")
            val result = engine.generate(content, EccLevel.M)
            (result as? QrGenerationResult.Success)?.let { success ->
                renderer.render(success.matrix, style, 256)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!isBuiltIn) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.template_custom_badge)) },
                        )
                    }
                }
                preview?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_template_preview),
                        modifier = Modifier
                            .fillMaxWidth(0.30f)
                            .aspectRatio(1f)
                            .align(Alignment.Top),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onApply) {
                    Text(stringResource(R.string.template_apply))
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.cd_delete_item_template),
                        )
                    }
                }
            }
        }
    }
}
