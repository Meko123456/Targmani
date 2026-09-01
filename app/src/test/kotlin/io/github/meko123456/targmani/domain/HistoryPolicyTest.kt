package io.github.meko123456.targmani.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryPolicyTest {

    private val enKa = TranslationDirection(Language.ENGLISH, Language.GEORGIAN)
    private val enAr = TranslationDirection(Language.ENGLISH, Language.ARABIC)
    private val t0 = 1_000_000L

    private fun head(text: String, at: Long = t0, dir: TranslationDirection = enKa, fav: Boolean = false) =
        HistoryHead(id = 7, sourceText = text, direction = dir, timestampMillis = at, favourite = fav)

    private fun candidate(text: String, at: Long = t0 + 1_000, dir: TranslationDirection = enKa, out: String = "translated") =
        HistoryCandidate(sourceText = text, translatedText = out, direction = dir, timestampMillis = at)

    @Test
    fun `blank input or empty translation is skipped`() {
        assertEquals(HistoryDecision.Skip, HistoryPolicy.decide(null, candidate("")))
        assertEquals(HistoryDecision.Skip, HistoryPolicy.decide(null, candidate("   ")))
        assertEquals(HistoryDecision.Skip, HistoryPolicy.decide(null, candidate("hello", out = "")))
    }

    @Test
    fun `the first entry is inserted`() {
        assertEquals(HistoryDecision.Insert, HistoryPolicy.decide(null, candidate("hello")))
    }

    @Test
    fun `re-translating the identical phrase is skipped`() {
        assertEquals(HistoryDecision.Skip, HistoryPolicy.decide(head("hello"), candidate("hello")))
        // trimming and case differences still count as identical
        assertEquals(HistoryDecision.Skip, HistoryPolicy.decide(head("hello"), candidate("  Hello ")))
    }

    @Test
    fun `a phrase still being typed replaces the head instead of piling up`() {
        // "good" -> "good morning": the user kept typing, so history should hold one entry
        val decision = HistoryPolicy.decide(head("good"), candidate("good morning"))
        assertEquals(HistoryDecision.Replace(7), decision)
    }

    @Test
    fun `deleting characters also counts as the same phrase`() {
        assertEquals(HistoryDecision.Replace(7), HistoryPolicy.decide(head("good morning"), candidate("good")))
    }

    @Test
    fun `a genuinely different phrase is inserted`() {
        assertEquals(HistoryDecision.Insert, HistoryPolicy.decide(head("good morning"), candidate("thank you")))
    }

    @Test
    fun `the same text in a different direction is a separate entry`() {
        assertEquals(HistoryDecision.Insert, HistoryPolicy.decide(head("hello", dir = enKa), candidate("hello", dir = enAr)))
    }

    @Test
    fun `typing again much later starts a new entry`() {
        val late = t0 + HistoryPolicy.CONTINUATION_WINDOW_MILLIS + 1
        assertEquals(HistoryDecision.Insert, HistoryPolicy.decide(head("good"), candidate("good morning", at = late)))
    }

    @Test
    fun `a favourite is never overwritten`() {
        assertEquals(HistoryDecision.Insert, HistoryPolicy.decide(head("good", fav = true), candidate("good morning")))
    }

    @Test
    fun `pruning keeps the newest N and never touches favourites`() {
        val entries = (1L..10L).map {
            HistoryHead(id = it, sourceText = "t$it", direction = enKa, timestampMillis = t0 + it, favourite = it == 2L)
        }
        val doomed = HistoryPolicy.prunable(entries, keep = 3)
        // 9 non-favourites, keep 3 newest (10, 9, 8) -> prune 7,6,5,4,3,1 (2 is starred)
        assertTrue(2L !in doomed)
        assertTrue(10L !in doomed && 9L !in doomed && 8L !in doomed)
        assertEquals(setOf(7L, 6L, 5L, 4L, 3L, 1L), doomed.toSet())
    }

    @Test
    fun `pruning an under-cap history removes nothing`() {
        val entries = (1L..3L).map { HistoryHead(it, "t$it", enKa, t0 + it, false) }
        assertTrue(HistoryPolicy.prunable(entries, keep = 10).isEmpty())
    }
}
