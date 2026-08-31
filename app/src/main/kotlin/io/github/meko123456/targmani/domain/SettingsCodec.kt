package io.github.meko123456.targmani.domain

/**
 * Plain-string encoding for the stored preferences — no serialization library for two fields.
 * A direction is `"<from>|<to>"` (BCP-47 codes). Anything unparsable (an old code, a language
 * we no longer offer, a same-language pair) falls back to [TranslationDirection.DEFAULT] rather
 * than throwing, so a bad stored value can never brick startup.
 */
object SettingsCodec {

    private const val SEPARATOR = '|'

    fun encodeDirection(direction: TranslationDirection): String =
        "${direction.from.code}$SEPARATOR${direction.to.code}"

    fun decodeDirection(text: String?): TranslationDirection {
        val parts = text?.split(SEPARATOR) ?: return TranslationDirection.DEFAULT
        if (parts.size != 2) return TranslationDirection.DEFAULT
        val from = Language.ofCode(parts[0]) ?: return TranslationDirection.DEFAULT
        val to = Language.ofCode(parts[1]) ?: return TranslationDirection.DEFAULT
        if (from == to) return TranslationDirection.DEFAULT
        return TranslationDirection(from, to)
    }
}
