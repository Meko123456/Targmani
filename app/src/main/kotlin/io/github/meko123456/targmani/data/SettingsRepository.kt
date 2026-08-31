package io.github.meko123456.targmani.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.meko123456.targmani.domain.SettingsCodec
import io.github.meko123456.targmani.domain.TranslationDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "targmani")

/** What the user chose last: the translation direction and whether model downloads need Wi-Fi. */
data class Settings(
    val direction: TranslationDirection = TranslationDirection.DEFAULT,
    val wifiOnlyDownloads: Boolean = false,
)

/**
 * Preferences-backed settings. Reads never fail: an unparsable direction falls back to the
 * default (see [SettingsCodec]), so the app always starts on a valid pair.
 */
class SettingsRepository(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            direction = SettingsCodec.decodeDirection(prefs[DIRECTION]),
            wifiOnlyDownloads = prefs[WIFI_ONLY] ?: false,
        )
    }

    suspend fun setDirection(direction: TranslationDirection) {
        store.edit { it[DIRECTION] = SettingsCodec.encodeDirection(direction) }
    }

    suspend fun setWifiOnlyDownloads(enabled: Boolean) {
        store.edit { it[WIFI_ONLY] = enabled }
    }

    private companion object {
        val DIRECTION = stringPreferencesKey("direction")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_downloads")
    }
}
