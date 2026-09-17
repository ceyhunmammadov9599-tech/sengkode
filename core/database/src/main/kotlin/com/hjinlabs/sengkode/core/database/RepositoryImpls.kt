package com.hjinlabs.sengkode.core.database

import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentType
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.repository.HistoryItem
import com.hjinlabs.sengkode.core.model.repository.HistoryRepository
import com.hjinlabs.sengkode.core.model.repository.TemplateRecord
import com.hjinlabs.sengkode.core.model.repository.TemplateRepository
import com.hjinlabs.sengkode.core.style.BuiltInTemplates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Single JSON codec for content/style snapshots (tolerant: forward compatible). */
object SnapshotCodec {
    val json: Json = Json { ignoreUnknownKeys = true }

    fun encodeContent(content: QrContent): String =
        json.encodeToString(QrContent.serializer(), content)

    fun decodeContent(jsonText: String): QrContent =
        json.decodeFromString(QrContent.serializer(), jsonText)

    fun encodeStyle(style: QrStyle): String =
        json.encodeToString(QrStyle.serializer(), style)

    fun decodeStyle(jsonText: String): QrStyle =
        json.decodeFromString(QrStyle.serializer(), jsonText)
}

class HistoryRepositoryImpl @Inject constructor(
    private val dao: HistoryDao,
) : HistoryRepository {

    /** Test seam for deterministic timestamps; production uses wall clock. */
    var clock: () -> Long = System::currentTimeMillis

    override fun observeAll(): Flow<List<HistoryItem>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: Long): HistoryItem? = dao.getById(id)?.toDomain()

    override suspend fun save(content: QrContent, style: QrStyle, title: String): Long =
        dao.insert(
            HistoryEntity(
                title = title.ifBlank { "Untitled" },
                contentJson = SnapshotCodec.encodeContent(content),
                styleJson = SnapshotCodec.encodeStyle(style),
                createdAtEpochMs = clock(),
            ),
        )

    override suspend fun setFavorite(id: Long, favorite: Boolean) =
        dao.setFavorite(id, favorite)

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun clearAll() = dao.deleteAll()

    private fun HistoryEntity.toDomain() = HistoryItem(
        id = id,
        title = title,
        content = SnapshotCodec.decodeContent(contentJson),
        style = SnapshotCodec.decodeStyle(styleJson),
        createdAtEpochMs = createdAtEpochMs,
        isFavorite = isFavorite,
    )
}

class TemplateRepositoryImpl @Inject constructor(
    private val dao: TemplateDao,
) : TemplateRepository {

    override fun observeAll(): Flow<List<TemplateRecord>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun ensureSeeded() {
        if (dao.count() > 0) return
        BuiltInTemplates.ALL.forEach { template ->
            dao.insert(
                TemplateEntity(
                    name = template.name,
                    description = template.description,
                    styleJson = SnapshotCodec.encodeStyle(template.style),
                    contentTypeHint = template.contentTypeHint.name,
                    isBuiltIn = true,
                ),
            )
        }
    }

    override suspend fun saveCustom(
        name: String,
        description: String,
        style: QrStyle,
        contentTypeHint: QrContentType,
    ): Long = dao.insert(
        TemplateEntity(
            name = name,
            description = description,
            styleJson = SnapshotCodec.encodeStyle(style),
            contentTypeHint = contentTypeHint.name,
            isBuiltIn = false,
        ),
    )

    override suspend fun deleteCustom(id: Long) {
        val entity = dao.getById(id)
            ?: throw IllegalArgumentException("Template $id does not exist")
        check(!entity.isBuiltIn) { "Built-in templates cannot be deleted" }
        dao.deleteById(id)
    }

    private fun TemplateEntity.toDomain() = TemplateRecord(
        id = id,
        name = name,
        description = description,
        style = SnapshotCodec.decodeStyle(styleJson),
        contentTypeHint = runCatching {
            QrContentType.valueOf(contentTypeHint)
        }.getOrDefault(QrContentType.TEXT),
        isBuiltIn = isBuiltIn,
    )
}
