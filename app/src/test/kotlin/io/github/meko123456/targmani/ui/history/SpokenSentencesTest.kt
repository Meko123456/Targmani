package io.github.meko123456.targmani.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class SpokenSentencesTest {

    @Test
    fun `a part that already ends a sentence gets no second stop`() {
        assertEquals(
            "English → ქართული. Good morning, how are you? დილა მშვიდობისა, როგორ ხარ?",
            spokenSentences("English → ქართული", "Good morning, how are you?", "დილა მშვიდობისა, როგორ ხარ?"),
        )
    }

    @Test
    fun `parts without an ending get a full stop`() {
        assertEquals(
            "ქართული → English. დილა მშვიდობისა. Good morning.",
            spokenSentences("ქართული → English", "დილა მშვიდობისა", "Good morning"),
        )
    }

    @Test
    fun `exclamations, ellipses and stray spaces are left as they are`() {
        assertEquals("Wow! Wait… Done.", spokenSentences("Wow!", " Wait… ", "Done."))
        assertEquals("Only.", spokenSentences("", "Only", "  "))
    }
}
