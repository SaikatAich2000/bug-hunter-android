package com.bughunter.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.authPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = AuthPrefs.FILE_NAME)

class AuthPrefs internal constructor(
    private val store: DataStore<Preferences>,
) {

    val lastKnownMe: Flow<String?> = store.data.map { it[KEY_LAST_KNOWN_ME] }
    val lockoutEpochMs: Flow<Long?> = store.data.map { it[KEY_LOCKOUT_EPOCH_MS] }
    val totpEnabledCached: Flow<Boolean> = store.data.map { it[KEY_TOTP_ENABLED_CACHED] ?: false }

    suspend fun setLastKnownMe(json: String?) {
        store.edit { p -> if (json == null) p.remove(KEY_LAST_KNOWN_ME) else p[KEY_LAST_KNOWN_ME] = json }
    }

    suspend fun setLockoutEpochMs(value: Long?) {
        store.edit { p -> if (value == null) p.remove(KEY_LOCKOUT_EPOCH_MS) else p[KEY_LOCKOUT_EPOCH_MS] = value }
    }

    suspend fun setTotpEnabledCached(value: Boolean) {
        store.edit { it[KEY_TOTP_ENABLED_CACHED] = value }
    }

    suspend fun clear() {
        store.edit { it.clear() }
    }

    companion object {
        const val FILE_NAME = "auth_prefs"

        private val KEY_LAST_KNOWN_ME = stringPreferencesKey("last_known_me")
        private val KEY_LOCKOUT_EPOCH_MS = longPreferencesKey("last_locked_out_until_epoch_ms")
        private val KEY_TOTP_ENABLED_CACHED = booleanPreferencesKey("totp_enabled_cached")
    }
}
