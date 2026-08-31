package io.github.meko123456.targmani.domain

/** Whether a language's on-device model is available, and what it's doing right now. */
enum class ModelState { NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED, DELETING }

/** One row of the model manager: a language and the state of its on-device model. */
data class ModelStatus(val language: Language, val state: ModelState) {
    val isReady: Boolean get() = state == ModelState.DOWNLOADED
    val isBusy: Boolean get() = state == ModelState.DOWNLOADING || state == ModelState.DELETING
}

/**
 * Port over per-language model storage, so the manager's logic is testable without ML Kit.
 * These are *language* models (not direction pairs): ML Kit downloads one per language and
 * any direction between two downloaded languages then works offline.
 */
interface ModelStore {
    /** Languages whose models are on the device right now. */
    suspend fun downloaded(): Set<Language>

    /** Download [language]'s model (no-op if present). */
    suspend fun download(language: Language, requireWifi: Boolean): Result<Unit>

    /** Delete [language]'s model to reclaim space. */
    suspend fun delete(language: Language): Result<Unit>
}

/**
 * Pure logic behind the model-manager screen: builds the rows and answers "can this direction
 * work offline?".
 *
 * ML Kit always keeps English as a pivot — every translation goes through it — so a non-English
 * pair needs three models (both languages *and* English). [missingFor] encodes exactly that,
 * which is what lets the UI tell the user *why* a Georgian→Arabic translation still needs a
 * download.
 */
object ModelPlanner {

    /** The pivot language ML Kit routes every translation through. */
    val PIVOT: Language = Language.ENGLISH

    /** A row per offered language, marking which are already downloaded. */
    fun rows(downloaded: Set<Language>, busy: Map<Language, ModelState> = emptyMap()): List<ModelStatus> =
        LanguageCatalog.languages.map { language ->
            val state = busy[language]
                ?: if (language in downloaded) ModelState.DOWNLOADED else ModelState.NOT_DOWNLOADED
            ModelStatus(language, state)
        }

    /**
     * Which models still need downloading before [direction] works offline — the two languages
     * plus the English pivot, minus whatever is already present. Empty means fully offline-ready.
     */
    fun missingFor(direction: TranslationDirection, downloaded: Set<Language>): Set<Language> =
        (setOf(direction.from, direction.to, PIVOT) - downloaded)

    /** True when [direction] can translate with no network at all. */
    fun isOfflineReady(direction: TranslationDirection, downloaded: Set<Language>): Boolean =
        missingFor(direction, downloaded).isEmpty()

    /**
     * Deleting a language's model breaks any direction that needs it. Deleting the English
     * pivot breaks *everything*, so the UI warns before that one.
     */
    fun deletingBreaksAllPairs(language: Language): Boolean = language == PIVOT
}
