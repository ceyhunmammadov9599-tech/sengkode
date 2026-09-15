package com.hjinlabs.sengkode.feature.generator

import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
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
        StudioViewModel(spy ?: engine, UnconfinedTestDispatcher())

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
}
