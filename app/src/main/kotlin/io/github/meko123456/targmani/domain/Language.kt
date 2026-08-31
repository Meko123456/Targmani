package io.github.meko123456.targmani.domain

/**
 * A language Targmani can translate, on-device. [code] is the BCP-47 tag ML Kit's
 * `TranslateLanguage` uses ("en", "ka", "ar"), kept as a plain string so this stays free of
 * ML Kit and fully unit-testable. [endonym] is the language's own name, shown in its own script.
 */
enum class Language(val code: String, val englishName: String, val endonym: String, val rtl: Boolean = false) {
    ENGLISH("en", "English", "English"),
    GEORGIAN("ka", "Georgian", "ქართული"),
    ARABIC("ar", "Arabic", "العربية", rtl = true),
    ;

    companion object {
        /** The language for a BCP-47 [code], or null if Targmani doesn't offer it. */
        fun ofCode(code: String?): Language? = entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}
