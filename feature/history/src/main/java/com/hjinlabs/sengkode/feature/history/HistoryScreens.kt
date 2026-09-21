package com.hjinlabs.sengkode.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.outlined.MoreVert
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.Dispatchers as UiDispatchers
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hjinlabs.sengkode.core.export.BatchPngExporter
import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.export.QrFileExporter
import com.hjinlabs.sengkode.core.model.repository.HistoryItem
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.type
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * History list. Rows show snapshots (title, type, date) - images
 * are never stored; the detail screen regenerates them through the
 * one pipeline. Single rendering path: preview == export.
 */
@Composable
fun HistoryScreen(
    onOpenDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var batchMenuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { ZxingQrEngine() }
    val renderer = remember { DrawListBitmapRenderer() }

    fun runBatchExport(list: List<HistoryItem>) {
        if (list.isEmpty()) {
            Toast.makeText(context, R.string.batch_none, Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = dir.resolve("sengkode-batch-${System.currentTimeMillis()}.zip")
            val result = BatchPngExporter(engine, renderer)
                .exportToZip(list.map { it.content to it.style }, file.outputStream())
            val message = context.getString(R.string.batch_exported, result.exportedCount) +
                if (result.skipped.isNotEmpty()) {
                    context.getString(R.string.batch_skipped, result.skipped.size)
                } else {
                    ""
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            QrFileExporter(context).shareZip(file)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            HistoryTopBar(
                onClearAll = { confirmClear = true },
                hasItems = items.isNotEmpty(),
                onExportAll = { runBatchExport(items) },
                onExportFavorites = {
                    runBatchExport(items.filter { it.isFavorite })
                },
                hasFavorites = items.any { it.isFavorite },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.history_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    HistoryRow(
                        item = item,
                        onClick = { onOpenDetail(item.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onDelete = { viewModel.delete(item) },
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; viewModel.clearAll() }) {
                    Text(stringResource(R.string.history_clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryTopBar(
    onClearAll: () -> Unit,
    hasItems: Boolean,
    onExportAll: () -> Unit,
    onExportFavorites: () -> Unit,
    hasFavorites: Boolean,
) {
    val menuLabel = stringResource(R.string.cd_batch_menu)
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.history_title)) },
        actions = {
            if (hasItems) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.semantics { contentDescription = menuLabel },
                    ) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.batch_export_all)) },
                            onClick = { menuOpen = false; onExportAll() },
                        )
                        if (hasFavorites) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.batch_export_favorites)) },
                                onClick = { menuOpen = false; onExportFavorites() },
                            )
                        }
                    }
                }
                IconButton(onClick = onClearAll) {
                    Icon(
                        Icons.Outlined.DeleteSweep,
                        contentDescription = stringResource(R.string.history_clear_all),
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val favoriteLabel = stringResource(R.string.cd_favorite_toggle)
    val deleteLabel = stringResource(R.string.cd_delete_item)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                Icon(
                    Icons.Outlined.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            headlineContent = { Text(item.title, maxLines = 1) },
            supportingContent = {
                Text(
                    "${item.content.type.name.lowercase().replaceFirstChar { it.uppercase() }} - " +
                        DateFormat.getDateInstance().format(Date(item.createdAtEpochMs)),
                    maxLines = 1,
                )
            },
            trailingContent = {
                Row {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.semantics { contentDescription = favoriteLabel },
                    ) {
                        Icon(
                            if (item.isFavorite) Icons.Outlined.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (item.isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.semantics { contentDescription = deleteLabel },
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
        )
    }
}

/**
 * Detail: regenerates the stored snapshot through the SAME engine +
 * renderer pair as the studio and export - never a second rendering
 * path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    onRegenerate: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val item by viewModel.detail.collectAsStateWithLifecycle()
    val engine = remember { ZxingQrEngine() }
    val renderer = remember { DrawListBitmapRenderer() }

    var preview by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(item) {
        val current = item ?: return@LaunchedEffect
        preview = withContext(Dispatchers.Default) {
            val result = engine.generate(current.content, EccLevel.M)
            (result as? QrGenerationResult.Success)?.let { success ->
                renderer.render(success.matrix, current.style, 512)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_cancel),
                        )
                    }
                },
                actions = {
                    item?.let { current ->
                        val favoriteLabel = stringResource(R.string.cd_favorite_toggle)
                        IconButton(
                            onClick = { viewModel.toggleFavorite(current) },
                            modifier = Modifier.semantics { contentDescription = favoriteLabel },
                        ) {
                            Icon(
                                if (current.isFavorite) Icons.Outlined.Favorite
                                else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
            ) {
                preview?.let { bitmap ->
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_saved_preview),
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .aspectRatio(1f)
                            .padding(16.dp),
                    )
                }
            }
            item?.let { current ->
                Text(
                    DateFormat.getDateTimeInstance().format(Date(current.createdAtEpochMs)),
                    style = MaterialTheme.typography.labelMedium,
                )
                Button(
                    onClick = {
                        viewModel.regenerate(current)
                        onRegenerate()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_regenerate))
                }
                OutlinedButton(
                    onClick = { viewModel.deleteCurrentDetail() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}
