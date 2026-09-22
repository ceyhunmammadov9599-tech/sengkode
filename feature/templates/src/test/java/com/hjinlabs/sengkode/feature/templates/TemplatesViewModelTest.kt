package com.hjinlabs.sengkode.feature.templates

import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentDefaults
import com.hjinlabs.sengkode.core.model.QrContentType
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore
import com.hjinlabs.sengkode.core.model.repository.TemplateRecord
import com.hjinlabs.sengkode.core.model.repository.TemplateRepository
import com.hjinlabs.sengkode.core.qr.QrEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Template contract: templates provide STYLE ONLY - applying one
 * seeds (default content for the hinted type, style) into the studio
 * session; generation logic is never duplicated here.
 *
 * Phase 7: TemplatesViewModel now receives QrEngine + DrawListBitmapRenderer
 * via DI. Tests use no-op fakes; the ViewModel contract is verified
 * without any rendering infrastructure.
 */
class TemplatesViewModelTest {

    private object FakeQrEngine : QrEngine {
        override fun generate(content: QrContent, ecc: EccLevel): QrGenerationResult =
            QrGenerationResult.Failure(
                com.hjinlabs.sengkode.core.model.QrError.Validation("fake"),
            )
    }

    private object FakeRenderer : DrawListBitmapRenderer()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeTemplateRepository : TemplateRepository {
        val records = MutableStateFlow<List<TemplateRecord>>(emptyList())
        var seeded = 0
        val deleted = mutableListOf<Long>()
        val saved = mutableListOf<String>()

        override fun observeAll(): Flow<List<TemplateRecord>> = records

        override suspend fun ensureSeeded() {
            seeded++
        }

        override suspend fun saveCustom(
            name: String,
            description: String,
            style: QrStyle,
            contentTypeHint: QrContentType,
        ): Long {
            saved.add(name)
            return saved.size.toLong()
        }

        override suspend fun deleteCustom(id: Long) {
            deleted.add(id)
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

    private fun template(id: Long, hint: QrContentType, style: QrStyle = QrStyle()) =
        TemplateRecord(
            id = id,
            name = "T$id",
            description = "d",
            style = style,
            contentTypeHint = hint,
            isBuiltIn = true,
        )

    private fun viewModel(
        repo: FakeTemplateRepository = FakeTemplateRepository(),
        store: RecordingRestoreStore = RecordingRestoreStore(),
    ) = TemplatesViewModel(repo, store, FakeQrEngine, FakeRenderer)

    @Test
    fun `init seeds the gallery exactly once`() = runTest {
        val repo = FakeTemplateRepository()
        viewModel(repo = repo)
        assertEquals(1, repo.seeded)
    }

    @Test
    fun `apply puts default content for the hinted type plus the style`() = runTest {
        val repo = FakeTemplateRepository()
        val store = RecordingRestoreStore()
        val viewModel = viewModel(repo = repo, store = store)
        val style = QrStyle(foregroundArgb = 0xFF00695CL)
        val record = template(1, QrContentType.WIFI, style)

        val returned = viewModel.apply(record)

        assertEquals(QrContentDefaults.defaultFor(QrContentType.WIFI), returned.first)
        assertEquals(style, returned.second)
        assertEquals(returned, store.puts.single())
    }

    @Test
    fun `apply for vcard hint seeds a vcard default`() = runTest {
        val store = RecordingRestoreStore()
        val viewModel = viewModel(store = store)
        val record = template(2, QrContentType.VCARD)

        val (content, _) = viewModel.apply(record)

        assertTrue(content is QrContent.VCard)
        assertEquals("", (content as QrContent.VCard).fullName)
    }

    @Test
    fun `deleteCustom forwards to the repository`() = runTest {
        val repo = FakeTemplateRepository()
        val viewModel = viewModel(repo = repo)
        viewModel.deleteCustom(template(4, QrContentType.TEXT))
        // Main is the Unconfined test dispatcher: the viewModelScope
        // launch runs eagerly, so the repository call is observable
        // immediately after.
        assertEquals(listOf(4L), repo.deleted)
    }

    @Test
    fun `engine and renderer are the injected instances`() = runTest {
        val vm = viewModel()
        assertTrue(vm.engine === FakeQrEngine)
        assertTrue(vm.renderer === FakeRenderer)
    }
}
