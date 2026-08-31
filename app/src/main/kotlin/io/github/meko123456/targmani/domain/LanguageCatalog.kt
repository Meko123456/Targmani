package io.github.meko123456.targmani.domain

/**
 * The languages Targmani exposes and the model pairs between them. Georgian, English and
 * Arabic — Merab's three — chosen because all three have on-device ML Kit models, so every
 * pairing works fully offline once its models are downloaded.
 */
object LanguageCatalog {

    /** Offered languages, in menu order (the two most-used first). */
    val languages: List<Language> = listOf(Language.ENGLISH, Language.GEORGIAN, Language.ARABIC)

    /** Every ordered source→target pair (no self-pairs). */
    fun directions(): List<TranslationDirection> =
        languages.flatMap { from -> languages.filter { it != from }.map { to -> TranslationDirection(from, to) } }

    /**
     * The distinct **unordered** language pairs whose models are needed, so model management
     * dedupes en↔ka with ka↔en. Each pair is the two languages sorted by code.
     */
    fun modelPairs(): List<Pair<Language, Language>> =
        languages.flatMap { a -> languages.filter { it.code > a.code }.map { b -> a to b } }
}
