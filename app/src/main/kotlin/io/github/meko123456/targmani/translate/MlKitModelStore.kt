package io.github.meko123456.targmani.translate

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import io.github.meko123456.targmani.domain.Language
import io.github.meko123456.targmani.domain.ModelStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * [ModelStore] over ML Kit's [RemoteModelManager]: lists, downloads and deletes the per-language
 * translation models stored on the device.
 */
class MlKitModelStore(private val io: CoroutineDispatcher = Dispatchers.IO) : ModelStore {

    private val manager = RemoteModelManager.getInstance()

    override suspend fun downloaded(): Set<Language> = withContext(io) {
        val models = runCatching { manager.getDownloadedModels(TranslateRemoteModel::class.java).await() }
            .getOrDefault(emptySet())
        // ML Kit reports every downloaded translate model; keep only the ones Targmani offers.
        models.mapNotNull { Language.ofCode(it.language) }.toSet()
    }

    override suspend fun download(language: Language, requireWifi: Boolean): Result<Unit> = withContext(io) {
        val conditions = DownloadConditions.Builder().apply { if (requireWifi) requireWifi() }.build()
        runCatching { manager.download(model(language), conditions).await() }.map { }
    }

    override suspend fun delete(language: Language): Result<Unit> = withContext(io) {
        runCatching { manager.deleteDownloadedModel(model(language)).await() }.map { }
    }

    private fun model(language: Language): TranslateRemoteModel =
        TranslateRemoteModel.Builder(
            requireNotNull(TranslateLanguage.fromLanguageTag(language.code)) {
                "ML Kit has no translation model for ${language.code}"
            },
        ).build()
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
    addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    addOnCanceledListener { if (cont.isActive) cont.cancel() }
}
