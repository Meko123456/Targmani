package io.github.meko123456.targmani.domain

/**
 * Maps a [Language] to the locale tag a text-to-speech engine expects, and records which
 * languages realistically have a voice installed.
 *
 * Kept pure so the mapping is unit-testable: the TTS engine itself is an Android type, but
 * choosing *what* to ask it for is plain logic. Georgian TTS voices are rare on Android
 * devices, so the UI must handle "no voice for this language" as a normal outcome rather than
 * an error — [isLikelySupported] says which languages to expect trouble with.
 */
object SpeechLocale {

    /** BCP-47 tag for the TTS engine. */
    fun tagFor(language: Language): String = language.code

    /**
     * Whether a device is likely to ship a voice for this language. Georgian is not part of the
     * standard Google TTS voice set, so speaking Georgian usually needs a third-party engine.
     */
    fun isLikelySupported(language: Language): Boolean = when (language) {
        Language.ENGLISH, Language.ARABIC -> true
        Language.GEORGIAN -> false
    }

    /** Message shown when the engine has no voice for [language]. */
    fun missingVoiceMessage(language: Language): String =
        "No ${language.englishName} voice is installed on this device."
}
