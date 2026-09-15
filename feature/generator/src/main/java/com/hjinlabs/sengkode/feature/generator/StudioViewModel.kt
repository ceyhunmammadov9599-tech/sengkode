package com.hjinlabs.sengkode.feature.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.model.QrStyle
import com.hjinlabs.sengkode.core.qr.QrEngine
import com.hjinlabs.sengkode.core.style.LogoEccPolicy
import com.hjinlabs.sengkode.core.style.ScanSafety
import com.hjinlabs.sengkode.core.style.ScanSafetyPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Immutable UI state (UDF): single source of truth for the studio.
 * The matrix is DERIVED state - regenerated from (content, ecc,
 * style); the safety verdict is derived from (style, matrix).
 */
data class StudioState(
    val content: QrContent = QrContent.Text(""),
    val ecc: EccLevel = EccLevel.DEFAULT,
    val style: QrStyle = QrStyle(),
    val result: QrGenerationResult? = null,
    val isGenerating: Boolean = false,
    val safety: ScanSafety = ScanSafetyPolicy.evaluate(QrStyle(), DEFAULT_MATRIX_WIDTH),
) {
    val matrix get() = (result as? QrGenerationResult.Success)?.matrix
    val error get() = (result as? QrGenerationResult.Failure)?.error

    /**
     * ECC after the logo policy: the level the engine is actually
     * asked for (never below the user's choice; upgraded for logos).
     */
    val effectiveEcc: EccLevel
        get() = LogoEccPolicy.effectiveEcc(ecc, style.logo)

    /** True when the style may be rendered (preview and export gate). */
    val canRender: Boolean get() = matrix != null && safety is ScanSafety.Safe

    companion object {
        const val DEFAULT_MATRIX_WIDTH = 33
    }
}

/**
 * MVVM + UDF: content/style changes flow DOWN into the state,
 * generation is debounced, the engine runs off the main thread.
 * Unsafe styles are never silently rendered: the safety verdict
 * travels in the state and the UI gates preview/export on it.
 */
@HiltViewModel
class StudioViewModel @Inject constructor(
    private val engine: QrEngine,
    private val generationDispatcher: CoroutineDispatcher,
) : ViewModel() {

    constructor(engine: QrEngine) : this(engine, Dispatchers.Default)

    private val _state = MutableStateFlow(StudioState())
    val state: StateFlow<StudioState> = _state.asStateFlow()

    private var generationJob: Job? = null

    fun updateContent(content: QrContent) {
        _state.update { it.copy(content = content) }
        scheduleGeneration()
    }

    fun updateEcc(ecc: EccLevel) {
        _state.update { it.copy(ecc = ecc) }
        scheduleGeneration()
    }

    fun setStyle(style: QrStyle) {
        updateStyle { style }
    }

    fun updateStyle(transform: (QrStyle) -> QrStyle) {
        _state.update { state ->
            val newStyle = transform(state.style)
            // A style change re-evaluates the safety verdict immediately
            // (pure, cheap) and may change the effective ECC (logo).
            state.copy(
                style = newStyle,
                safety = ScanSafetyPolicy.evaluate(
                    newStyle,
                    state.matrix?.width ?: StudioState.DEFAULT_MATRIX_WIDTH,
                ),
            )
        }
        scheduleGeneration()
    }

    private fun scheduleGeneration() {
        _state.update { it.copy(isGenerating = true) }
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            delay(GENERATION_DEBOUNCE_MS)
            val snapshot = _state.value
            val result = withContext(generationDispatcher) {
                engine.generate(snapshot.content, snapshot.effectiveEcc)
            }
            _state.update { state ->
                state.copy(
                    result = result,
                    isGenerating = false,
                    safety = ScanSafetyPolicy.evaluate(
                        state.style,
                        (result as? QrGenerationResult.Success)?.matrix?.width
                            ?: StudioState.DEFAULT_MATRIX_WIDTH,
                    ),
                )
            }
        }
    }

    companion object {
        const val GENERATION_DEBOUNCE_MS = 150L
    }
}
