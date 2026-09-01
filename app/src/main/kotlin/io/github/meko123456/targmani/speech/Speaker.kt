package io.github.meko123456.targmani.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.SpeechLocale
import java.util.Locale

/**
 * Speaks translated text with the platform TTS engine. Initialisation is asynchronous, so a call
 * made before the engine is ready is queued and replayed once it reports success.
 *
 * Speaking is best-effort by design: many devices ship no Georgian voice, so [speak] reports that
 * through [onUnavailable] instead of throwing, and the UI shows a plain message.
 */
class Speaker(context: Context, private val onUnavailable: (Language) -> Unit = {}) : AutoCloseable {

    private var ready = false
    private var pending: Pair<String, Language>? = null

    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        val queued = pending
        pending = null
        if (ready && queued != null) speak(queued.first, queued.second)
    }

    fun speak(text: String, language: Language) {
        if (text.isBlank()) return
        if (!ready) {
            pending = text to language
            return
        }
        val result = tts.setLanguage(Locale.forLanguageTag(SpeechLocale.tagFor(language)))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            onUnavailable(language)
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        if (ready) tts.stop()
    }

    override fun close() {
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val UTTERANCE_ID = "targmani-translation"
    }
}
