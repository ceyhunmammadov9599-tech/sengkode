package com.hjinlabs.sengkode.core.model.repository

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentType
import com.hjinlabs.sengkode.core.model.QrStyle
import kotlinx.coroutines.flow.Flow

/**
 * Domain records and repository contracts. ViewModels depend on these
 * interfaces only - the Room implementation stays isolated in
 * :core:database behind Hilt bindings. Storage stores CONTENT and
 * STYLE snapshots, never rendered bitmaps: every image is
 * regenerated through the one existing pipeline
 * (matrix -> style renderer -> backend), so preview == history
 * render == export.
 */
data class HistoryItem(
    val id: Long,
    val title: String,
    val content: QrContent,
    val style: QrStyle,
    val createdAtEpochMs: Long,
    val isFavorite: Boolean,
)

interface HistoryRepository {
    /** Newest first, favorites pinned to the top. */
    fun observeAll(): Flow<List<HistoryItem>>

    suspend fun get(id: Long): HistoryItem?

    /** Snapshots content + style; returns the new row id. */
    suspend fun save(content: QrContent, style: QrStyle, title: String): Long

    suspend fun setFavorite(id: Long, favorite: Boolean)

    suspend fun delete(id: Long)

    /** Privacy action: wipes ALL history. The UI owns the confirmation. */
    suspend fun clearAll()
}

data class TemplateRecord(
    val id: Long,
    val name: String,
    val description: String,
    val style: QrStyle,
    val contentTypeHint: QrContentType,
    val isBuiltIn: Boolean,
)

interface TemplateRepository {
    fun observeAll(): Flow<List<TemplateRecord>>

    /** Idempotent seeding of the built-in gallery on first run. */
    suspend fun ensureSeeded()

    suspend fun saveCustom(
        name: String,
        description: String,
        style: QrStyle,
        contentTypeHint: QrContentType,
    ): Long

    /** Built-ins are not deletable; deleting one throws. */
    suspend fun deleteCustom(id: Long)
}

/**
 * Session-scope handoff between features (history regenerate,
 * template apply -> studio). Tiny by design: one pending
 * (content, style) pair, consumed exactly once by the studio.
 */
interface StudioRestoreStore {
    fun put(content: QrContent, style: QrStyle)

    fun consume(): Pair<QrContent, QrStyle>?
}
