package com.hjinlabs.sengkode.feature.history

import androidx.lifecycle.SavedStateHandle
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.WifiEncryption
import com.hjinlabs.sengkode.core.model.repository.HistoryItem
import com.hjinlabs.sengkode.core.model.repository.HistoryRepository
import com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore
import com.hjinlabs.sengkode.core.model.repository.TemplateRecord
import com.hjinlabs.sengkode.core.model.repository.TemplateRepository
import com.hjinlabs.sengkode.core.model.QrContentType
import app.cash.turbine.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * History UDF contract: list projection + repository commands +
 * exactly-once session handoff. JVM-only - rendering stays in the
 * screen layer and is covered by the styled round-trip suites.
 */
class HistoryViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeHistoryRepository : HistoryRepository {
        val items = MutableStateFlow<List<HistoryItem>>(emptyList())
        val favorites = mutableListOf<Pair<Long, Boolean>>()
        val deleted = mutableListOf<Long>()
        var cleared = false

        override fun observeAll(): Flow<List<HistoryItem>> = items

        override suspend fun get(id: Long): HistoryItem? =
            items.value.firstOrNull { it.id == id }

        override suspend fun save(content: QrContent, style: QrStyle, title: String) = 0L

        override suspend fun setFavorite(id: Long, favorite: Boolean) {
            favorites.add(id to favorite)
        }

        override suspend fun delete(id: Long) {
            deleted.add(id)
        }

        override suspend fun clearAll() {
            cleared = true
        }
    }

    private class RecordingRestoreStore : StudioRestoreStore {
        private var pending: Pair<QrContent, QrStyle>? = null
        val puts = mutableListOf<Pair<QrContent, QrStyle>>()

        override fun put(content: QrContent, style: QrStyle) {
            pending = content to style
            puts.add(pending!!)
        }

        override fun consume(): Pair<QrContent, QrStyle>? = pending.also { pending = null }
    }

    private fun repoItem(
        id: Long,
        title: String = "Item $id",
        favorite: Boolean = false,
    ) = HistoryItem(
        id = id,
        title = title,
        content = QrContent.Wifi("Net$id", "pw", WifiEncryption.WPA, false),
        style = QrStyle(),
        createdAtEpochMs = 1_000L * id,
        isFavorite = favorite,
    )

    @Test
    fun `list state projects the repository flow`() = runTest {
        val repo = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repo, RecordingRestoreStore(), SavedStateHandle())

        // WhileSubscribed(5000): the upstream only runs while someone
        // collects - exactly what a real screen does.
        viewModel.items.test {
            assertEquals(emptyList<Long>(), awaitItem().map { it.id })
            repo.items.value = listOf(repoItem(1, favorite = true), repoItem(2))
            assertEquals(listOf(1L, 2L), awaitItem().map { it.id })
            assertEquals(true, viewModel.items.value.first().isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail loads by id from saved state`() = runTest {
        val repo = FakeHistoryRepository()
        repo.items.value = listOf(repoItem(7))
        val viewModel = HistoryViewModel(
            repo, RecordingRestoreStore(),
            SavedStateHandle(mapOf(HistoryViewModel.KEY_ITEM_ID to 7L)),
        )
        // Unconfined dispatcher lets the init load complete eagerly.
        assertEquals(7L, viewModel.detail.value?.id)
    }

    @Test
    fun `detail is null without an id`() = runTest {
        val viewModel = HistoryViewModel(FakeHistoryRepository(), RecordingRestoreStore(), SavedStateHandle())
        assertNull(viewModel.detail.value)
    }

    @Test
    fun `toggleFavorite inverts the stored flag`() = runTest {
        val repo = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repo, RecordingRestoreStore(), SavedStateHandle())
        viewModel.toggleFavorite(repoItem(3, favorite = false))
        assertEquals(listOf(3L to true), repo.favorites)
        viewModel.toggleFavorite(repoItem(3, favorite = true))
        assertEquals(listOf(3L to true, 3L to false), repo.favorites)
    }

    @Test
    fun `delete and clearAll reach the repository`() = runTest {
        val repo = FakeHistoryRepository()
        val viewModel = HistoryViewModel(repo, RecordingRestoreStore(), SavedStateHandle())
        viewModel.delete(repoItem(5))
        assertEquals(listOf(5L), repo.deleted)
        viewModel.deleteCurrentDetail()
        viewModel.clearAll()
        assertTrue(repo.cleared)
    }

    @Test
    fun `regenerate hands the snapshot to the studio session once`() = runTest {
        val store = RecordingRestoreStore()
        val viewModel = HistoryViewModel(FakeHistoryRepository(), store, SavedStateHandle())
        val item = repoItem(9)
        viewModel.regenerate(item)
        viewModel.regenerate(item)

        assertEquals(2, store.puts.size)
        // Exactly-once consumption: the studio gets the latest pair, a
        // second consume returns null.
        assertEquals(item.content, store.consume()?.first)
        assertNull(store.consume())
    }
}
