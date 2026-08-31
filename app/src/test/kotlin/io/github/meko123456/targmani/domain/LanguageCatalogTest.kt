package io.github.meko123456.targmani.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageCatalogTest {

    @Test
    fun `catalog offers the three languages, English first`() {
        assertEquals(listOf(Language.ENGLISH, Language.GEORGIAN, Language.ARABIC), LanguageCatalog.languages)
    }

    @Test
    fun `directions are every ordered pair with no self-pairs`() {
        val d = LanguageCatalog.directions()
        assertEquals(6, d.size) // 3 * 2
        assertTrue(d.none { it.from == it.to })
        assertTrue(d.contains(TranslationDirection(Language.ENGLISH, Language.GEORGIAN)))
        assertTrue(d.contains(TranslationDirection(Language.ARABIC, Language.ENGLISH)))
        assertEquals(d.size, d.toSet().size) // no duplicates
    }

    @Test
    fun `model pairs are the three unordered pairs, deduped`() {
        val pairs = LanguageCatalog.modelPairs()
        assertEquals(3, pairs.size) // en-ka, en-ar, ka-ar (choose(3,2))
        // each pair sorted by code, so en(=en) before ka/ar, ar before en/ka
        assertTrue(pairs.all { it.first.code < it.second.code })
    }
}
