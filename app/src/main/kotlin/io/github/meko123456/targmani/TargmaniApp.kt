package io.github.meko123456.targmani

import android.app.Application
import android.content.Context
import io.github.meko123456.targmani.domain.Translator
import io.github.meko123456.targmani.translate.MlKitTranslator

/** Application-scoped object graph. Small app, no DI framework needed. */
class TargmaniApp : Application() {
    /** One shared engine; ML Kit clients are cached inside it and released on process death. */
    val translator: Translator by lazy { MlKitTranslator() }
}

val Context.targmaniApp: TargmaniApp
    get() = applicationContext as TargmaniApp
