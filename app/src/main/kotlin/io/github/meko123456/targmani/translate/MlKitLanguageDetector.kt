package io.github.meko123456.targmani.translate

import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.LanguageIdentification
import io.github.meko123456.targmani.domain.LanguageDetector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [LanguageDetector] backed by ML Kit language identification — fully on-device, no model
 * download needed (the identifier ships with the library).
 */
class MlKitLanguageDetector(private val io: CoroutineDispatcher = Dispatchers.IO) : LanguageDetector, AutoCloseable {

    private val client = LanguageIdentification.getClient()

    override suspend fun detect(text: String): String? = withContext(io) {
        if (text.isBlank()) return@withContext null
        runCatching { client.identifyLanguage(text).await() }.getOrNull()
    }

    override fun close() = client.close()
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
    addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    addOnCanceledListener { if (cont.isActive) cont.cancel() }
}
