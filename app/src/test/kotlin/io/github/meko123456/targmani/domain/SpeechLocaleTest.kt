package io.github.meko123456.targmani.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechLocaleTest {

    @Test
    fun `tags are the BCP-47 codes the engine expects`() {
        assertEquals("en", SpeechLocale.tagFor(Language.ENGLISH))
        assertEquals("ka", SpeechLocale.tagFor(Language.GEORGIAN))
        assertEquals("ar", SpeechLocale.tagFor(Language.ARABIC))
    }

    @Test
    fun `Georgian is flagged as unlikely to have a voice`() {
        // Georgian is not in the standard Android TTS voice set, so the UI must expect a miss.
        assertFalse(SpeechLocale.isLikelySupported(Language.GEORGIAN))
        assertTrue(SpeechLocale.isLikelySupported(Language.ENGLISH))
        assertTrue(SpeechLocale.isLikelySupported(Language.ARABIC))
    }

    @Test
    fun `the missing-voice message names the language`() {
        assertEquals("No Georgian voice is installed on this device.", SpeechLocale.missingVoiceMessage(Language.GEORGIAN))
    }
}
