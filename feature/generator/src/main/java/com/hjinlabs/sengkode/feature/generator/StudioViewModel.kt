package com.hjinlabs.sengkode.feature.generator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hjinlabs.sengkode.core.model.EccLevel
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrGenerationResult
import com.hjinlabs.sengkode.core.qr.QrEngine
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
 * The matrix is DERIVED state - regenerated from (content, ecc),
 * never edited directly.
 */
data class StudioState(
    val content: QrContent = QrContent.Text(""),
    val ecc: EccLevel = EccLevel.DEFAULT,
    val result: QrGenerationResult? = null,
    val isGenerating: Boolean = false,
) {
    val matrix get() = (result as? QrGenerationResult.Success)?.matrix
    val error get() = (result as? QrGenerationResult.Failure)?.error
}

/**
 * MVVM + UDF: content changes flow DOWN into the state, generation is
 * debounced so fast typing recomputes once, and the engine runs off
 * the main thread. The UI never touches the engine directly.
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

    private fun scheduleGeneration() {
        _state.update { it.copy(isGenerating = true) }
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            delay(GENERATION_DEBOUNCE_MS)
            val content = _state.value.content
            val ecc = _state.value.ecc
            val result = withContext(generationDispatcher) {
                engine.generate(content, ecc)
            }
            _state.update { it.copy(result = result, isGenerating = false) }
        }
    }

    companion object {
        const val GENERATION_DEBOUNCE_MS = 150L
    }
}
