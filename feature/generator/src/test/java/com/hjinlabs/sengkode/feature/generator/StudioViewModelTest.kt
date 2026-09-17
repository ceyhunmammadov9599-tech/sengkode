package com.hjinlabs.sengkode.feature.generator

import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.model.QrError
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.WifiEncryption
import com.hjinlabs.sengkode.core.qr.QrEngine
import com.hjinlabs.sengkode.core.qr.ZxingQrEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudioViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()
    private val engine = ZxingQrEngine()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(spy: QrEngine? = null): StudioViewModel =
        StudioViewModel(
            spy ?: engine,
            FakeHistoryRepository(),
            FakeTemplateRepository(),
            RecordingRestoreStore(),
            UnconfinedTestDispatcher(),
        )

    @Test
    fun `valid content produces a matrix after the debounce`() = runTest {
        val viewModel = vm()
        viewModel.updateContent(QrContent.Text("SENGKODE"))
        assertEquals(true, viewModel.state.value.isGenerating)

        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
        val state = viewModel.state.value
        assertTrue(state.result is QrGenerationResult.Success)
        assertEquals("SENGKODE", (state.result as QrGenerationResult.Success).payload)
        assertEquals(false, state.isGenerating)
        assertTrue(state.matrix != null)
    }

    @Test
    fun `invalid content surfaces a typed validation error`() = runTest {
        val viewModel = vm()
        viewModel.updateContent(
            QrContent.Wifi("HomeNet", "short", WifiEncryption.WPA, false),
        )
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
        val error = viewModel.state.value.error
        assertTrue(error is QrError.Validation)
    }

    @Test
    fun `oversized content surfaces a typed capacity error`() = runTest {
        val viewModel = vm()
        viewModel.updateContent(QrContent.Text("x".repeat(2400)))
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
        assertTrue(viewModel.state.value.error is QrError.Capacity)
    }

    @Test
    fun `ecc change regenerates with the new level`() = runTest {
        val viewModel = vm()
        viewModel.updateContent(QrContent.Text("hello"))
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
        viewModel.updateEcc(EccLevel.H)
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
        val success = viewModel.state.value.result as QrGenerationResult.Success
        assertEquals(EccLevel.H, success.eccLevel)
    }

    @Test
    fun `rapid edits trigger exactly one generation`() = runTest {
        val generationCountingEngine = object : QrEngine {
            var calls = 0
            override fun generate(content: QrContent, ecc: EccLevel): QrGenerationResult {
                calls++
                return engine.generate(content, ecc)
            }
        }
        val viewModel = vm(generationCountingEngine)
        viewModel.updateContent(QrContent.Text("a"))
        viewModel.updateContent(QrContent.Text("ab"))
        viewModel.updateContent(QrContent.Text("abc"))
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
        assertEquals(1, generationCountingEngine.calls)
        assertEquals(
            "abc",
            (viewModel.state.value.result as QrGenerationResult.Success).payload,
        )
    }

    // ---- Phase 2: style, scan safety, logo ECC -------------------------

// ---- Phase 3: history/template integration fakes --------------------

private class FakeHistoryRepository : com.hjinlabs.sengkode.core.model.repository.HistoryRepository {
    val saved = mutableListOf<Triple<QrContent, QrStyle, String>>()
    private val flow = kotlinx.coroutines.flow.MutableStateFlow(
        emptyList<com.hjinlabs.sengkode.core.model.repository.HistoryItem>(),
    )
    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<com.hjinlabs.sengkode.core.model.repository.HistoryItem>> = flow
    override suspend fun get(id: Long) = flow.value.firstOrNull { it.id == id }
    override suspend fun save(content: QrContent, style: QrStyle, title: String): Long {
        saved.add(Triple(content, style, title))
        return saved.size.toLong()
    }
    override suspend fun setFavorite(id: Long, favorite: Boolean) = Unit
    override suspend fun delete(id: Long) = Unit
    override suspend fun clearAll() = Unit
}

private class FakeTemplateRepository :
    com.hjinlabs.sengkode.core.model.repository.TemplateRepository {
    val custom = mutableListOf<String>()
    override fun observeAll(): kotlinx.coroutines.flow.Flow<List<com.hjinlabs.sengkode.core.model.repository.TemplateRecord>> =
        kotlinx.coroutines.flow.flowOf(emptyList())
    override suspend fun ensureSeeded() = Unit
    override suspend fun saveCustom(
        name: String,
        description: String,
        style: QrStyle,
        contentTypeHint: com.hjinlabs.sengkode.core.model.QrContentType,
    ): Long {
        custom.add(name)
        return custom.size.toLong()
    }
    override suspend fun deleteCustom(id: Long) = Unit
}

class RecordingRestoreStore : com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore {
    private var pair: Pair<QrContent, QrStyle>? = null
    val puts = mutableListOf<Pair<QrContent, QrStyle>>()
    override fun put(content: QrContent, style: QrStyle) {
        pair = content to style
        puts.add(pair!!)
    }
    override fun consume(): Pair<QrContent, QrStyle>? = pair.also { pair = null }
}

private class EccSpy(private val real: QrEngine) : QrEngine {
    val usedEcc = mutableListOf<EccLevel>()
    override fun generate(content: QrContent, ecc: EccLevel): QrGenerationResult {
        usedEcc.add(ecc)
        return real.generate(content, ecc)
    }
}

@Test
fun `style change regenerates`() = runTest {
    val spy = EccSpy(engine)
    val viewModel = StudioViewModel(
            spy,
            FakeHistoryRepository(),
            FakeTemplateRepository(),
            RecordingRestoreStore(),
            UnconfinedTestDispatcher(),
        )
    viewModel.updateContent(QrContent.Text("styled"))
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    assertEquals(1, spy.usedEcc.size)

    viewModel.updateStyle { it.copy(eyeShape = com.hjinlabs.sengkode.core.model.EyeShape.ROUNDED) }
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    assertEquals(2, spy.usedEcc.size)
}

@Test
fun `logo upgrades effective ecc for generation`() = runTest {
    val spy = EccSpy(engine)
    val viewModel = StudioViewModel(
            spy,
            FakeHistoryRepository(),
            FakeTemplateRepository(),
            RecordingRestoreStore(),
            UnconfinedTestDispatcher(),
        )
    viewModel.updateContent(QrContent.Text("logo ecc upgrade 0123456789"))
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    assertEquals(EccLevel.M, spy.usedEcc.last())

    viewModel.updateStyle {
        it.copy(logo = com.hjinlabs.sengkode.core.model.LogoSpec(sizeFraction = 0.20f))
    }
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    // 20% logo -> H; user's M is kept as a floor, never lowered.
    assertEquals(EccLevel.H, spy.usedEcc.last())
    assertEquals(EccLevel.H, viewModel.state.value.effectiveEcc)
}

@Test
fun `user ecc choice is a floor - logo never lowers it`() = runTest {
    val spy = EccSpy(engine)
    val viewModel = StudioViewModel(
            spy,
            FakeHistoryRepository(),
            FakeTemplateRepository(),
            RecordingRestoreStore(),
            UnconfinedTestDispatcher(),
        )
    viewModel.updateContent(QrContent.Text("floor ecc"))
    viewModel.updateEcc(EccLevel.Q)
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    viewModel.updateStyle {
        it.copy(logo = com.hjinlabs.sengkode.core.model.LogoSpec(sizeFraction = 0.12f))
    }
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    assertEquals(EccLevel.Q, spy.usedEcc.last())
}

@Test
fun `unsafe style blocks rendering but generation still succeeds`() = runTest {
    val viewModel = vm()
    viewModel.updateContent(QrContent.Text("unsafe style"))
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    viewModel.updateStyle {
        it.copy(
            foregroundArgb = 0xFFFFFFFFL,
            backgroundArgb = 0xFF000000L,
        )
    }
    val state = viewModel.state.value
    // Matrix exists (generation is style-independent), but rendering
    // is gated: inverted colors are never silently shown or exported.
    assertTrue(state.matrix != null)
    assertTrue(state.safety is com.hjinlabs.sengkode.core.style.ScanSafety.Unsafe)
    assertEquals(false, state.canRender)
}

@Test
fun `quiet zone violation is reported unsafe`() = runTest {
    val viewModel = vm()
    viewModel.updateContent(QrContent.Text("quiet zone"))
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    viewModel.updateStyle { it.copy(quietZoneModules = 2) }
    assertTrue(
        viewModel.state.value.safety is com.hjinlabs.sengkode.core.style.ScanSafety.Unsafe,
    )
}

@Test
fun `safe style reports safe and allows rendering`() = runTest {
    val viewModel = vm()
    viewModel.updateContent(QrContent.Text("safe style"))
    advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)
    viewModel.updateStyle {
        it.copy(moduleShape = com.hjinlabs.sengkode.core.model.ModuleShape.DOT)
    }
    val state = viewModel.state.value
    assertTrue(state.safety is com.hjinlabs.sengkode.core.style.ScanSafety.Safe)
    assertEquals(true, state.canRender)
}

    // ---- Phase 3: restore + persistence actions --------------------

    @Test
    fun `consumeRestore applies a pending history or template pair`() = runTest {
        val store = RecordingRestoreStore()
        val viewModel = StudioViewModel(
            engine, FakeHistoryRepository(), FakeTemplateRepository(), store,
            UnconfinedTestDispatcher(),
        )
        val content = QrContent.Text("restored from history")
        val style = QrStyle(
            moduleShape = com.hjinlabs.sengkode.core.model.ModuleShape.DOT,
        )
        store.put(content, style)

        viewModel.consumeRestore()
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)

        val state = viewModel.state.value
        assertEquals(content, state.content)
        assertEquals(style, state.style)
    }

    @Test
    fun `consumeRestore with nothing pending is a no-op`() = runTest {
        val store = RecordingRestoreStore()
        val viewModel = StudioViewModel(
            engine, FakeHistoryRepository(), FakeTemplateRepository(), store,
            UnconfinedTestDispatcher(),
        )
        viewModel.updateContent(QrContent.Text("mine"))
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)

        viewModel.consumeRestore()
        val state = viewModel.state.value
        assertEquals(QrContent.Text("mine"), state.content)
    }

    @Test
    fun `saveToHistory snapshots the current content and style`() = runTest {
        val repo = FakeHistoryRepository()
        val viewModel = StudioViewModel(
            engine, repo, FakeTemplateRepository(), RecordingRestoreStore(),
            UnconfinedTestDispatcher(),
        )
        val content = QrContent.Wifi("Net", "pw", com.hjinlabs.sengkode.core.model.WifiEncryption.WPA, false)
        viewModel.updateContent(content)
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)

        viewModel.saveToHistory()
        advanceTimeBy(50)

        assertEquals(1, repo.saved.size)
        assertEquals(content, repo.saved[0].first)
        assertEquals(QrStyle(), repo.saved[0].second)
        assertEquals("Wi-Fi Net", repo.saved[0].third)
    }

    @Test
    fun `saveAsTemplate persists the current style with content type`() = runTest {
        val templates = FakeTemplateRepository()
        val viewModel = StudioViewModel(
            engine, FakeHistoryRepository(), templates, RecordingRestoreStore(),
            UnconfinedTestDispatcher(),
        )
        viewModel.updateContent(QrContent.Url("https://hjinlabs.app"))
        advanceTimeBy(StudioViewModel.GENERATION_DEBOUNCE_MS + 1)

        viewModel.saveAsTemplate("")
        advanceTimeBy(50)

        assertEquals(listOf("My style"), templates.custom)
    }

    @Test
    fun `restore events are delivered`() = runTest {
        val store = RecordingRestoreStore()
        val viewModel = StudioViewModel(
            engine, FakeHistoryRepository(), FakeTemplateRepository(), store,
            UnconfinedTestDispatcher(),
        )
        store.put(QrContent.Text("x"), QrStyle())
        viewModel.consumeRestore()
        // consume exactly once: a second consume must be a no-op.
        store.put(QrContent.Text("y"), QrStyle())
        store.consume() // consumed outside the VM, like a second session
        viewModel.consumeRestore()
        assertEquals(QrContent.Text("x"), viewModel.state.value.content)
    }
}