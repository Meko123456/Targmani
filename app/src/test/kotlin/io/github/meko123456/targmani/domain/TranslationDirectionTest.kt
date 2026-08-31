package io.github.meko123456.targmani.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationDirectionTest {

    @Test
    fun `default is English to Georgian`() {
        assertEquals(Language.ENGLISH, TranslationDirection.DEFAULT.from)
        assertEquals(Language.GEORGIAN, TranslationDirection.DEFAULT.to)
    }

    @Test
    fun `swapping reverses source and target`() {
        val d = TranslationDirection(Language.ENGLISH, Language.ARABIC)
        assertEquals(TranslationDirection(Language.ARABIC, Language.ENGLISH), d.swapped())
        assertEquals(d, d.swapped().swapped())
    }

    @Test
    fun `rtl flags follow the source and target languages`() {
        val enToAr = TranslationDirection(Language.ENGLISH, Language.ARABIC)
        assertFalse(enToAr.sourceRtl)
        assertTrue(enToAr.targetRtl)
        val arToEn = enToAr.swapped()
        assertTrue(arToEn.sourceRtl)
        assertFalse(arToEn.targetRtl)
    }

    @Test
    fun `a direction cannot have the same source and target`() {
        assertThrows(IllegalArgumentException::class.java) {
            TranslationDirection(Language.GEORGIAN, Language.GEORGIAN)
        }
    }
}
