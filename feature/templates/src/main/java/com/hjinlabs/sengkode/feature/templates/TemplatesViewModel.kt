package com.hjinlabs.sengkode.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.model.QrContent
import com.hjinlabs.sengkode.core.model.QrContentDefaults
import com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore
import com.hjinlabs.sengkode.core.model.repository.TemplateRecord
import com.hjinlabs.sengkode.core.model.repository.TemplateRepository
import com.hjinlabs.sengkode.core.qr.QrEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Templates provide QrStyle configuration ONLY. Applying a template
 * seeds the studio through the session restore store; generation,
 * safety policy and rendering all stay in the single existing
 * pipeline - zero duplicated QR logic.
 */
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: TemplateRepository,
    private val restoreStore: StudioRestoreStore,
    val engine: QrEngine,
    val renderer: DrawListBitmapRenderer,
) : ViewModel() {

    val templates: StateFlow<List<TemplateRecord>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }

    /**
     * Applies a template: hands (default content for the hinted
     * type, template style) to the studio session. Returns the pair
     * for navigation - the caller decides where to go.
     */
    fun apply(template: TemplateRecord): Pair<QrContent, com.hjinlabs.sengkode.core.model.QrStyle> {
        val content = QrContentDefaults.defaultFor(template.contentTypeHint)
        restoreStore.put(content, template.style)
        return content to template.style
    }

    fun deleteCustom(template: TemplateRecord) {
        viewModelScope.launch {
            runCatching { repository.deleteCustom(template.id) }
        }
    }
}
