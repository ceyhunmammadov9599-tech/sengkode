package com.hjinlabs.sengkode.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hjinlabs.sengkode.core.export.DrawListBitmapRenderer
import com.hjinlabs.sengkode.core.model.repository.HistoryItem
import com.hjinlabs.sengkode.core.model.repository.HistoryRepository
import com.hjinlabs.sengkode.core.model.repository.StudioRestoreStore
import com.hjinlabs.sengkode.core.qr.QrEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * List + detail in one small UDF ViewModel: the list is a direct
 * projection of the repository Flow; every action (favorite,
 * delete, clear) is a repository command. Rendering never happens
 * here - screens render through the shared pipeline.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
    private val restoreStore: StudioRestoreStore,
    savedStateHandle: SavedStateHandle,
    val engine: QrEngine,
    val renderer: DrawListBitmapRenderer,
) : ViewModel() {

    val items: StateFlow<List<HistoryItem>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val detailItemId: Long = savedStateHandle.get<Long>(KEY_ITEM_ID) ?: 0L

    private val _detail = MutableStateFlow<HistoryItem?>(null)
    val detail: StateFlow<HistoryItem?> = _detail

    init {
        if (detailItemId != 0L) {
            viewModelScope.launch {
                _detail.value = repository.get(detailItemId)
            }
        }
    }

    fun toggleFavorite(item: HistoryItem) {
        viewModelScope.launch { repository.setFavorite(item.id, !item.isFavorite) }
    }

    fun delete(item: HistoryItem) {
        viewModelScope.launch { repository.delete(item.id) }
    }

    fun deleteCurrentDetail() {
        _detail.value?.let { delete(it) }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    /**
     * Regeneration hands the stored snapshot to the studio session;
     * the studio then runs the normal pipeline (engine -> policy ->
     * renderer). One rendering path for everything.
     */
    fun regenerate(item: HistoryItem) {
        restoreStore.put(item.content, item.style)
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
    }
}
