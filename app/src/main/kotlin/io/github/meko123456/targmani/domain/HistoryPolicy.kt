package io.github.meko123456.targmani.domain

/**
 * A translation the app is about to record.
 */
data class HistoryCandidate(
    val sourceText: String,
    val translatedText: String,
    val direction: TranslationDirection,
    val timestampMillis: Long,
)

/** The previous entry already in history, reduced to what the decision needs. */
data class HistoryHead(
    val id: Long,
    val sourceText: String,
    val direction: TranslationDirection,
    val timestampMillis: Long,
    val favourite: Boolean,
)

/** What to do with a candidate. */
sealed interface HistoryDecision {
    data object Skip : HistoryDecision
    data object Insert : HistoryDecision
    /** The head was a partial version of the same phrase — overwrite it instead of piling up. */
    data class Replace(val id: Long) : HistoryDecision
}

/**
 * Decides whether a finished translation is worth recording.
 *
 * Because translation runs on a debounce while the user types, a naive "insert every result"
 * would flood history with half-typed fragments: "goo", "good mor", "good morning". This policy
 * collapses that run — if the newest text simply extends (or trims) the head entry in the same
 * direction, and it happened recently, the head is replaced rather than a new row added. A
 * favourite is never replaced, since the user deliberately kept it.
 */
object HistoryPolicy {

    /** How long a continuing edit still counts as "the same phrase being typed". */
    const val CONTINUATION_WINDOW_MILLIS: Long = 60_000L

    fun decide(head: HistoryHead?, candidate: HistoryCandidate): HistoryDecision {
        val source = candidate.sourceText.trim()
        if (source.isEmpty() || candidate.translatedText.isBlank()) return HistoryDecision.Skip
        if (head == null) return HistoryDecision.Insert

        val sameDirection = head.direction == candidate.direction
        val headSource = head.sourceText.trim()
        if (sameDirection && headSource.equals(source, ignoreCase = true)) return HistoryDecision.Skip

        val withinWindow = candidate.timestampMillis - head.timestampMillis in 0..CONTINUATION_WINDOW_MILLIS
        val continuesTyping = source.startsWith(headSource, ignoreCase = true) ||
            headSource.startsWith(source, ignoreCase = true)
        return if (sameDirection && withinWindow && continuesTyping && !head.favourite) {
            HistoryDecision.Replace(head.id)
        } else {
            HistoryDecision.Insert
        }
    }

    /** Newest-first ids to prune so at most [keep] non-favourite entries remain. Favourites are never pruned. */
    fun prunable(entries: List<HistoryHead>, keep: Int): List<Long> =
        entries.filterNot { it.favourite }
            .sortedByDescending { it.timestampMillis }
            .drop(keep)
            .map { it.id }
}
