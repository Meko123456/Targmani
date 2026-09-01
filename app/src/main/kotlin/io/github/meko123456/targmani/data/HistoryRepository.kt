package io.github.meko123456.targmani.data

import io.github.meko123456.targmani.domain.HistoryCandidate
import io.github.meko123456.targmani.domain.HistoryDecision
import io.github.meko123456.targmani.domain.HistoryHead
import io.github.meko123456.targmani.domain.HistoryPolicy
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.TranslationDirection
import kotlinx.coroutines.flow.Flow

/**
 * Stores finished translations, applying [HistoryPolicy] so a run of half-typed fragments
 * collapses into one entry and history stays capped.
 */
class HistoryRepository(private val dao: TranslationDao, private val keep: Int = DEFAULT_KEEP) {

    fun observeAll(): Flow<List<TranslationRecord>> = dao.observeAll()

    fun observeFavourites(): Flow<List<TranslationRecord>> = dao.observeFavourites()

    /** Record a finished translation if the policy says it is worth keeping. */
    suspend fun record(candidate: HistoryCandidate) {
        when (val decision = HistoryPolicy.decide(dao.head()?.toHead(), candidate)) {
            HistoryDecision.Skip -> return
            HistoryDecision.Insert -> dao.insert(candidate.toRecord())
            is HistoryDecision.Replace -> dao.update(candidate.toRecord().copy(id = decision.id))
        }
        prune()
    }

    suspend fun setFavourite(id: Long, favourite: Boolean) = dao.setFavourite(id, favourite)

    suspend fun delete(id: Long) = dao.deleteById(id)

    /** Clears history but keeps starred entries. */
    suspend fun clearHistory() = dao.clearUnfavourited()

    private suspend fun prune() {
        val doomed = HistoryPolicy.prunable(dao.all().map { it.toHead() }, keep)
        if (doomed.isNotEmpty()) dao.deleteByIds(doomed)
    }

    private companion object {
        const val DEFAULT_KEEP = 200
    }
}

/** BCP-47 codes back into a direction; null when a stored code is no longer offered. */
fun TranslationRecord.directionOrNull(): TranslationDirection? {
    val from = Language.ofCode(fromCode) ?: return null
    val to = Language.ofCode(toCode) ?: return null
    if (from == to) return null
    return TranslationDirection(from, to)
}

private fun TranslationRecord.toHead() = HistoryHead(
    id = id,
    sourceText = sourceText,
    direction = directionOrNull() ?: TranslationDirection.DEFAULT,
    timestampMillis = timestampMillis,
    favourite = favourite,
)

private fun HistoryCandidate.toRecord() = TranslationRecord(
    sourceText = sourceText.trim(),
    translatedText = translatedText,
    fromCode = direction.from.code,
    toCode = direction.to.code,
    timestampMillis = timestampMillis,
)
