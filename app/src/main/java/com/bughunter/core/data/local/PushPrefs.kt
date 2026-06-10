package com.bughunter.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.pushPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = PushPrefs.FILE_NAME)

/**
 * Persistent state for the push subsystem.
 *
 *  - lastRegisteredToken : the FCM token the server most recently
 *    confirmed it has for this device. We compare against the live
 *    FirebaseMessaging token on cold start; if it changed, we
 *    re-register. Avoids hammering /api/devices/register every launch.
 *
 *  - permissionPromptShown : true once we've shown the in-app
 *    rationale dialog at least once. We do NOT use this to suppress
 *    re-asking after a soft-deny — the system will throttle that — but
 *    we DO use it to avoid blasting the dialog on every cold launch.
 *
 *  - permissionPermanentlyDenied : the user picked "Don't ask again".
 *    Subsequent prompts must show the "open Settings" shortcut instead
 *    of re-requesting (the system will silently no-op a re-request).
 */
class PushPrefs internal constructor(
    private val store: DataStore<Preferences>,
) {

    val lastRegisteredToken: Flow<String?> = store.data.map { it[KEY_LAST_REGISTERED_TOKEN] }

    val permissionPromptShown: Flow<Boolean> = store.data.map { it[KEY_PERMISSION_PROMPT_SHOWN] ?: false }

    val permissionPermanentlyDenied: Flow<Boolean> =
        store.data.map { it[KEY_PERMISSION_DENIED_HARD] ?: false }

    suspend fun setLastRegisteredToken(value: String?) {
        store.edit { p ->
            if (value == null) p.remove(KEY_LAST_REGISTERED_TOKEN)
            else p[KEY_LAST_REGISTERED_TOKEN] = value
        }
    }

    suspend fun setPermissionPromptShown(value: Boolean) {
        store.edit { it[KEY_PERMISSION_PROMPT_SHOWN] = value }
    }

    suspend fun setPermissionPermanentlyDenied(value: Boolean) {
        store.edit { it[KEY_PERMISSION_DENIED_HARD] = value }
    }

    suspend fun clear() {
        // Called on logout so the next user doesn't inherit this user's
        // "we already registered this token" optimisation. The token
        // itself remains valid in Firebase — the backend just won't
        // have a row for it tied to the new user until they log in.
        store.edit { it.clear() }
    }

    companion object {
        const val FILE_NAME = "push_prefs"

        private val KEY_LAST_REGISTERED_TOKEN = stringPreferencesKey("last_registered_token")
        private val KEY_PERMISSION_PROMPT_SHOWN = booleanPreferencesKey("permission_prompt_shown")
        private val KEY_PERMISSION_DENIED_HARD = booleanPreferencesKey("permission_permanently_denied")
    }
}
