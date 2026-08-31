package io.github.meko123456.targmani.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsCodecTest {

    @Test
    fun `direction round-trips`() {
        val d = TranslationDirection(Language.ARABIC, Language.GEORGIAN)
        assertEquals("ar|ka", SettingsCodec.encodeDirection(d))
        assertEquals(d, SettingsCodec.decodeDirection("ar|ka"))
    }

    @Test
    fun `missing or malformed values fall back to the default`() {
        val def = TranslationDirection.DEFAULT
        assertEquals(def, SettingsCodec.decodeDirection(null))
        assertEquals(def, SettingsCodec.decodeDirection(""))
        assertEquals(def, SettingsCodec.decodeDirection("en"))
        assertEquals(def, SettingsCodec.decodeDirection("en|ka|ar"))
    }

    @Test
    fun `unknown language codes fall back to the default`() {
        assertEquals(TranslationDirection.DEFAULT, SettingsCodec.decodeDirection("en|fr"))
        assertEquals(TranslationDirection.DEFAULT, SettingsCodec.decodeDirection("xx|ka"))
    }

    @Test
    fun `a stored same-language pair falls back instead of throwing`() {
        assertEquals(TranslationDirection.DEFAULT, SettingsCodec.decodeDirection("ka|ka"))
    }
}
