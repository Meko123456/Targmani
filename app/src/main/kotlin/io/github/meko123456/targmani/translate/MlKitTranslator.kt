package io.github.meko123456.targmani.translate

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.TranslationDirection
import io.github.meko123456.targmani.domain.Translator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.mlkit.nl.translate.Translator as MlTranslator

/**
 * [Translator] backed by ML Kit's on-device translation. Translation runs locally; the network
 * is used only by [download] to fetch a language model once. ML Kit clients are cached per
 * direction (creating one is not free) and released in [close]; the models themselves stay on
 * disk between runs.
 */
class MlKitTranslator(private val io: CoroutineDispatcher = Dispatchers.IO) : Translator, AutoCloseable {

    private val clients = ConcurrentHashMap<TranslationDirection, MlTranslator>()
    private val modelManager = RemoteModelManager.getInstance()

    private fun clientFor(direction: TranslationDirection): MlTranslator =
        clients.getOrPut(direction) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(mlkitTag(direction.from))
                .setTargetLanguage(mlkitTag(direction.to))
                .build()
            Translation.getClient(options)
        }

    override suspend fun isReady(direction: TranslationDirection): Boolean = withContext(io) {
        modelDownloaded(direction.from) && modelDownloaded(direction.to)
    }

    override suspend fun download(direction: TranslationDirection, requireWifi: Boolean): Result<Unit> =
        withContext(io) {
            val conditions = DownloadConditions.Builder().apply { if (requireWifi) requireWifi() }.build()
            runCatching { clientFor(direction).downloadModelIfNeeded(conditions).await() }.map { }
        }

    override suspend fun translate(text: String, direction: TranslationDirection): Result<String> =
        withContext(io) {
            if (text.isBlank()) return@withContext Result.success("")
            // No implicit download here: the contract is that translate fails when models are
            // missing (the UI downloads first, with progress). ML Kit surfaces that as a failure.
            runCatching { clientFor(direction).translate(text).await() }
        }

    override fun close() {
        clients.values.forEach { it.close() }
        clients.clear()
    }

    private suspend fun modelDownloaded(language: Language): Boolean {
        val model = TranslateRemoteModel.Builder(mlkitTag(language)).build()
        return modelManager.isModelDownloaded(model).await()
    }

    private fun mlkitTag(language: Language): String =
        requireNotNull(TranslateLanguage.fromLanguageTag(language.code)) {
            "ML Kit has no translation model for ${language.code}"
        }
}

/** Bridge a Play-services [Task] into a coroutine without pulling in kotlinx-coroutines-play-services. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
    addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    addOnCanceledListener { if (cont.isActive) cont.cancel() }
}
