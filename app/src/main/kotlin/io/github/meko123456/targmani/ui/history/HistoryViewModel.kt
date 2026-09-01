package io.github.meko123456.targmani.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.targmani.data.HistoryRepository
import io.github.meko123456.targmani.data.TranslationRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** History can show everything or only the starred entries. */
enum class HistoryFilter { ALL, FAVOURITES }

data class HistoryUiState(
    val entries: List<TranslationRecord> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
)

class HistoryViewModel(private val repository: HistoryRepository) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)

    val state: StateFlow<HistoryUiState> =
        combine(repository.observeAll(), filter) { all, active ->
            HistoryUiState(
                entries = if (active == HistoryFilter.FAVOURITES) all.filter { it.favourite } else all,
                filter = active,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setFilter(value: HistoryFilter) { filter.value = value }

    fun toggleFavourite(record: TranslationRecord) {
        viewModelScope.launch { repository.setFavourite(record.id, !record.favourite) }
    }

    fun delete(record: TranslationRecord) {
        viewModelScope.launch { repository.delete(record.id) }
    }

    /** Clears history but keeps starred entries — the destructive action is confirmed in the UI. */
    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }
}
