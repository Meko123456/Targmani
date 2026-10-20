package io.github.meko123456.targmani

import android.content.Intent

/**
 * The text another app handed over: picked out of it and sent with the selection menu's
 * "Targmani" (PROCESS_TEXT), or shared to Targmani (SEND). Null for anything else, the launcher's
 * MAIN included.
 */
internal object IncomingText {
    fun from(intent: Intent?): String? = when (intent?.action) {
        Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
        Intent.ACTION_SEND -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
        else -> null
    }?.toString()?.takeIf { it.isNotBlank() }
}

/** One handover, numbered so the same text sent twice is still a second handover. */
internal data class Handover(val text: String, val number: Int)
