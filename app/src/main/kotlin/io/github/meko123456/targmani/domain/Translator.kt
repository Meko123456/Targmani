package io.github.meko123456.targmani.domain

/**
 * Port over the on-device translation engine, so screens and view models depend on this
 * interface (testable with a fake) rather than on ML Kit directly. The ML Kit-backed
 * implementation lands in #5.
 */
interface Translator {
    /** True once both language models for [direction] are downloaded and ready. */
    suspend fun isReady(direction: TranslationDirection): Boolean

    /** Download whatever models [direction] needs (no-op if already present). */
    suspend fun download(direction: TranslationDirection, requireWifi: Boolean = false): Result<Unit>

    /** Translate [text] in [direction]. Fails if the models aren't downloaded. */
    suspend fun translate(text: String, direction: TranslationDirection): Result<String>
}
