package io.github.meko123456.targmani.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelPlannerTest {

    private val en = Language.ENGLISH
    private val ka = Language.GEORGIAN
    private val ar = Language.ARABIC

    @Test
    fun `rows cover every offered language and mark the downloaded ones`() {
        val rows = ModelPlanner.rows(downloaded = setOf(ka))
        assertEquals(LanguageCatalog.languages, rows.map { it.language })
        assertEquals(ModelState.DOWNLOADED, rows.first { it.language == ka }.state)
        assertEquals(ModelState.NOT_DOWNLOADED, rows.first { it.language == en }.state)
    }

    @Test
    fun `an in-flight state overrides the stored one`() {
        val rows = ModelPlanner.rows(downloaded = setOf(ka), busy = mapOf(en to ModelState.DOWNLOADING, ka to ModelState.DELETING))
        assertEquals(ModelState.DOWNLOADING, rows.first { it.language == en }.state)
        assertEquals(ModelState.DELETING, rows.first { it.language == ka }.state)
        assertTrue(rows.first { it.language == en }.isBusy)
    }

    @Test
    fun `an English pair needs both its languages`() {
        assertEquals(setOf(en, ka), ModelPlanner.missingFor(TranslationDirection(en, ka), emptySet()))
        assertEquals(setOf(ka), ModelPlanner.missingFor(TranslationDirection(en, ka), setOf(en)))
        assertTrue(ModelPlanner.missingFor(TranslationDirection(en, ka), setOf(en, ka)).isEmpty())
    }

    @Test
    fun `a non-English pair also needs the English pivot`() {
        // Georgian -> Arabic routes through English, so all three models are required.
        assertEquals(setOf(ka, ar, en), ModelPlanner.missingFor(TranslationDirection(ka, ar), emptySet()))
        assertEquals(setOf(en), ModelPlanner.missingFor(TranslationDirection(ka, ar), setOf(ka, ar)))
        assertTrue(ModelPlanner.missingFor(TranslationDirection(ka, ar), setOf(ka, ar, en)).isEmpty())
    }

    @Test
    fun `offline readiness follows the missing set`() {
        assertFalse(ModelPlanner.isOfflineReady(TranslationDirection(ka, ar), setOf(ka, ar)))
        assertTrue(ModelPlanner.isOfflineReady(TranslationDirection(ka, ar), setOf(ka, ar, en)))
        assertTrue(ModelPlanner.isOfflineReady(TranslationDirection(en, ka), setOf(en, ka)))
    }

    @Test
    fun `deleting English breaks every pair, deleting another does not`() {
        assertTrue(ModelPlanner.deletingBreaksAllPairs(Language.ENGLISH))
        assertFalse(ModelPlanner.deletingBreaksAllPairs(Language.GEORGIAN))
        assertFalse(ModelPlanner.deletingBreaksAllPairs(Language.ARABIC))
    }
}
