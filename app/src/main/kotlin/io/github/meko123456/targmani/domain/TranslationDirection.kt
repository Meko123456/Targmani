package io.github.meko123456.targmani.domain

/**
 * A source→target translation direction. [from] and [to] must differ.
 */
data class TranslationDirection(val from: Language, val to: Language) {
    init {
        require(from != to) { "source and target must differ (both ${from.code})" }
    }

    /** The reverse direction (the language-swap button). */
    fun swapped(): TranslationDirection = TranslationDirection(to, from)

    /** Whether the entered/translated text should lay out right-to-left. */
    val sourceRtl: Boolean get() = from.rtl
    val targetRtl: Boolean get() = to.rtl

    companion object {
        /** Sensible first launch: English → Georgian. */
        val DEFAULT: TranslationDirection = TranslationDirection(Language.ENGLISH, Language.GEORGIAN)
    }
}
