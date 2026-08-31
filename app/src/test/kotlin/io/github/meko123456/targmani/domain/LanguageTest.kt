package io.github.meko123456.targmani.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTest {

    @Test
    fun `codes match ML Kit BCP-47 tags`() {
        assertEquals("en", Language.ENGLISH.code)
        assertEquals("ka", Language.GEORGIAN.code)
        assertEquals("ar", Language.ARABIC.code)
    }

    @Test
    fun `only Arabic is right-to-left`() {
        assertTrue(Language.ARABIC.rtl)
        assertFalse(Language.ENGLISH.rtl)
        assertFalse(Language.GEORGIAN.rtl)
    }

    @Test
    fun `endonyms are in the language's own script`() {
        assertEquals("ქართული", Language.GEORGIAN.endonym)
        assertEquals("العربية", Language.ARABIC.endonym)
    }

    @Test
    fun `ofCode is case-insensitive and rejects unknowns`() {
        assertEquals(Language.GEORGIAN, Language.ofCode("ka"))
        assertEquals(Language.GEORGIAN, Language.ofCode("KA"))
        assertNull(Language.ofCode("fr"))
        assertNull(Language.ofCode(null))
        assertNull(Language.ofCode(""))
    }
}
