package io.github.meko123456.targmani.domain

/**
 * Port over on-device language identification, so the mapping logic below can be tested
 * without ML Kit.
 */
interface LanguageDetector {
    /** BCP-47 tag of the detected language, or null when undetermined. */
    suspend fun detect(text: String): String?
}

/**
 * Turns a raw identification result into a direction change. ML Kit can return languages
 * Targmani doesn't offer (or "und" for undetermined) — in those cases we keep the current
 * direction rather than guessing, so auto-detect can never strand the user on a pair they
 * didn't choose.
 */
object DetectionMapper {

    /** ML Kit's "undetermined" tag. */
    const val UNDETERMINED = "und"

    /**
     * @return the direction to switch to, or null to leave the current one alone.
     *   When the detected language is already the target, the direction is swapped so the
     *   user's text translates *out of* the language they actually typed.
     */
    fun directionFor(detectedTag: String?, current: TranslationDirection): TranslationDirection? {
        if (detectedTag == null || detectedTag.equals(UNDETERMINED, ignoreCase = true)) return null
        // ML Kit may return a regioned tag ("en-US"); the base language is what we match on.
        val base = detectedTag.substringBefore('-')
        val detected = Language.ofCode(base) ?: return null
        return when (detected) {
            current.from -> null // already correct
            current.to -> current.swapped()
            else -> TranslationDirection(detected, current.to)
        }
    }
}
