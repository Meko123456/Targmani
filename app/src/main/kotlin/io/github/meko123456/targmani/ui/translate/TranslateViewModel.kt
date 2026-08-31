package io.github.meko123456.targmani.ui.translate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.meko123456.targmani.data.SettingsRepository
import io.github.meko123456.targmani.domain.DetectionMapper
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.LanguageDetector
import io.github.meko123456.targmani.domain.TranslationDirection
import io.github.meko123456.targmani.domain.Translator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the translate screen is doing right now. */
sealed interface TranslateStatus {
    data object Idle : TranslateStatus
    data object Downloading : TranslateStatus
    data object Translating : TranslateStatus
    data class Error(val message: String) : TranslateStatus
}

data class TranslateUiState(
    val direction: TranslationDirection = TranslationDirection.DEFAULT,
    val input: String = "",
    val output: String = "",
    val status: TranslateStatus = TranslateStatus.Idle,
)

/**
 * Drives the translate screen: debounced live translation over the [Translator] port, with a
 * download step (and its own status) the first time a direction's models are missing. Each new
 * keystroke or direction change cancels the in-flight translation, so only the latest wins.
 */
class TranslateViewModel(
    private val translator: Translator,
    private val settings: SettingsRepository? = null,
    private val detector: LanguageDetector? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(TranslateUiState())
    val state: StateFlow<TranslateUiState> = _state.asStateFlow()

    private var translateJob: Job? = null

    /** Downloads need Wi-Fi only if the user asked for that; read with the stored settings. */
    private var requireWifi: Boolean = false

    init {
        // Restore the last direction so the app opens where the user left off.
        settings?.let { repo ->
            viewModelScope.launch {
                val stored = repo.settings.first()
                requireWifi = stored.wifiOnlyDownloads
                _state.update { it.copy(direction = stored.direction) }
            }
        }
    }

    fun onInputChange(text: String) {
        _state.update { it.copy(input = text) }
        schedule()
    }

    /** Pick a source language; if it collides with the current target, swap so the pair stays valid. */
    fun onSourceLanguage(language: Language) {
        val cur = _state.value.direction
        setDirection(if (language == cur.to) TranslationDirection(language, cur.from) else TranslationDirection(language, cur.to))
    }

    /** Pick a target language; if it collides with the current source, swap so the pair stays valid. */
    fun onTargetLanguage(language: Language) {
        val cur = _state.value.direction
        setDirection(if (language == cur.from) TranslationDirection(cur.to, language) else TranslationDirection(cur.from, language))
    }

    /**
     * Identify the typed text's language and switch the source to it. A language Targmani
     * doesn't offer (or an undetermined result) leaves the direction untouched — see
     * [DetectionMapper].
     */
    fun detectSourceLanguage() {
        val detector = detector ?: return
        val text = _state.value.input
        if (text.isBlank()) return
        viewModelScope.launch {
            val tag = detector.detect(text)
            DetectionMapper.directionFor(tag, _state.value.direction)?.let(::setDirection)
        }
    }

    /** Swap languages and text, then re-translate. */
    fun swap() {
        _state.update {
            it.copy(direction = it.direction.swapped(), input = it.output, output = it.input)
        }
        settings?.let { repo -> viewModelScope.launch { repo.setDirection(_state.value.direction) } }
        schedule()
    }

    private fun setDirection(direction: TranslationDirection) {
        _state.update { it.copy(direction = direction) }
        settings?.let { repo -> viewModelScope.launch { repo.setDirection(direction) } }
        schedule()
    }

    private fun schedule() {
        translateJob?.cancel()
        val snapshot = _state.value
        if (snapshot.input.isBlank()) {
            _state.update { it.copy(output = "", status = TranslateStatus.Idle) }
            return
        }
        translateJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            if (!translator.isReady(snapshot.direction)) {
                _state.update { it.copy(status = TranslateStatus.Downloading) }
                val downloaded = translator.download(snapshot.direction, requireWifi)
                if (downloaded.isFailure) {
                    _state.update { it.copy(status = TranslateStatus.Error(DOWNLOAD_FAILED)) }
                    return@launch
                }
            }
            _state.update { it.copy(status = TranslateStatus.Translating) }
            translator.translate(snapshot.input, snapshot.direction)
                .onSuccess { out -> _state.update { it.copy(output = out, status = TranslateStatus.Idle) } }
                .onFailure { _state.update { it.copy(status = TranslateStatus.Error(TRANSLATE_FAILED)) } }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 400L
        const val DOWNLOAD_FAILED = "Couldn't download the language model. Check your connection and try again."
        const val TRANSLATE_FAILED = "Translation failed. Try again."
    }
}
