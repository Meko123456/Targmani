package io.github.meko123456.targmani

import android.app.Application
import android.content.Context
import io.github.meko123456.targmani.data.HistoryRepository
import io.github.meko123456.targmani.data.SettingsRepository
import io.github.meko123456.targmani.data.TargmaniDatabase
import io.github.meko123456.targmani.domain.LanguageDetector
import io.github.meko123456.targmani.domain.ModelStore
import io.github.meko123456.targmani.domain.Translator
import io.github.meko123456.targmani.translate.MlKitLanguageDetector
import io.github.meko123456.targmani.translate.MlKitModelStore
import io.github.meko123456.targmani.translate.MlKitTranslator

/** Application-scoped object graph. Small app, no DI framework needed. */
class TargmaniApp : Application() {
    /** One shared engine; ML Kit clients are cached inside it and released on process death. */
    val translator: Translator by lazy { MlKitTranslator() }

    /** Persisted direction + download preferences. */
    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    /** On-device language identification (ships with the library — no model download). */
    val detector: LanguageDetector by lazy { MlKitLanguageDetector() }

    /** Per-language on-device model storage (list / download / delete). */
    val modelStore: ModelStore by lazy { MlKitModelStore() }

    /** Recent translations and starred phrases. */
    val history: HistoryRepository by lazy { HistoryRepository(TargmaniDatabase.get(this).translationDao()) }
}

val Context.targmaniApp: TargmaniApp
    get() = applicationContext as TargmaniApp
