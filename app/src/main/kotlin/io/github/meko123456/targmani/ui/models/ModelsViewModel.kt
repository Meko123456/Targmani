package io.github.meko123456.targmani.ui.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.targmani.data.SettingsRepository
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.ModelPlanner
import io.github.meko123456.targmani.domain.ModelState
import io.github.meko123456.targmani.domain.ModelStatus
import io.github.meko123456.targmani.domain.ModelStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelsUiState(
    val rows: List<ModelStatus> = emptyList(),
    val wifiOnly: Boolean = false,
    val error: String? = null,
    val loading: Boolean = true,
)

/**
 * Drives the model manager: lists each language's on-device model and downloads or deletes it.
 * In-flight work is tracked per language so a row can show a spinner while the rest stay usable.
 */
class ModelsViewModel(
    private val store: ModelStore,
    private val settings: SettingsRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(ModelsUiState())
    val state: StateFlow<ModelsUiState> = _state.asStateFlow()

    private val busy = mutableMapOf<Language, ModelState>()

    init {
        refresh()
        settings?.let { repo ->
            viewModelScope.launch { _state.update { it.copy(wifiOnly = repo.settings.first().wifiOnlyDownloads) } }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val downloaded = store.downloaded()
            _state.update { it.copy(rows = ModelPlanner.rows(downloaded, busy), loading = false) }
        }
    }

    fun download(language: Language) {
        if (busy.containsKey(language)) return
        mark(language, ModelState.DOWNLOADING)
        viewModelScope.launch {
            val result = store.download(language, _state.value.wifiOnly)
            unmark(language)
            if (result.isFailure) {
                _state.update { it.copy(error = "Couldn't download ${language.englishName}. Check your connection.") }
            }
            refresh()
        }
    }

    fun delete(language: Language) {
        if (busy.containsKey(language)) return
        mark(language, ModelState.DELETING)
        viewModelScope.launch {
            val result = store.delete(language)
            unmark(language)
            if (result.isFailure) {
                _state.update { it.copy(error = "Couldn't delete ${language.englishName}.") }
            }
            refresh()
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        _state.update { it.copy(wifiOnly = enabled) }
        settings?.let { repo -> viewModelScope.launch { repo.setWifiOnlyDownloads(enabled) } }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun mark(language: Language, state: ModelState) {
        busy[language] = state
        _state.update { it.copy(rows = ModelPlanner.rows(it.rows.filter { r -> r.isReady }.map { r -> r.language }.toSet(), busy)) }
    }

    private fun unmark(language: Language) {
        busy.remove(language)
    }
}
