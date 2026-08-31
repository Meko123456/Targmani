package io.github.meko123456.targmani.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetectionMapperTest {

    private val enToKa = TranslationDirection(Language.ENGLISH, Language.GEORGIAN)

    @Test
    fun `undetermined or null leaves the direction alone`() {
        assertNull(DetectionMapper.directionFor(null, enToKa))
        assertNull(DetectionMapper.directionFor("und", enToKa))
        assertNull(DetectionMapper.directionFor("UND", enToKa))
    }

    @Test
    fun `a language we do not offer leaves the direction alone`() {
        assertNull(DetectionMapper.directionFor("fr", enToKa))
        assertNull(DetectionMapper.directionFor("zh", enToKa))
    }

    @Test
    fun `detecting the current source is a no-op`() {
        assertNull(DetectionMapper.directionFor("en", enToKa))
    }

    @Test
    fun `detecting the current target swaps the direction`() {
        assertEquals(enToKa.swapped(), DetectionMapper.directionFor("ka", enToKa))
    }

    @Test
    fun `detecting a third offered language becomes the new source`() {
        assertEquals(
            TranslationDirection(Language.ARABIC, Language.GEORGIAN),
            DetectionMapper.directionFor("ar", enToKa),
        )
    }

    @Test
    fun `regioned tags match on the base language`() {
        assertNull(DetectionMapper.directionFor("en-US", enToKa))
        assertEquals(enToKa.swapped(), DetectionMapper.directionFor("ka-GE", enToKa))
    }
}
