package com.hjinlabs.sengkode.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Storage stores CONTENT and STYLE SNAPSHOTS as JSON - never rendered
 * bitmaps. Images are always regenerated through the single existing
 * pipeline, so history/detail/export share one rendering path.
 * Local-only by design: no network permission exists in the app.
 */
@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val contentJson: String,
    val styleJson: String,
    val createdAtEpochMs: Long,
    val isFavorite: Boolean = false,
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String,
    val styleJson: String,
    /** QrContentType.name - the content type the template is designed for. */
    val contentTypeHint: String,
    val isBuiltIn: Boolean,
)
