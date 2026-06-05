package com.bughunter.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bughunter.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.appPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = AppPrefs.FILE_NAME)

class AppPrefs internal constructor(
    private val store: DataStore<Preferences>,
) {

    val themeMode: Flow<ThemeMode> = store.data.map { p ->
        when (p[KEY_THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val defaultNewType: Flow<String> = store.data.map { p -> p[KEY_DEFAULT_NEW_TYPE] ?: "Bug" }

    val navCollapsed: Flow<Boolean> = store.data.map { p -> p[KEY_NAV_COLLAPSED] ?: false }

    val baseUrl: Flow<String> = store.data.map { p ->
        p[KEY_BASE_URL] ?: BASE_URL_DEFAULT
    }

    val lastKnownOrgId: Flow<Int?> = store.data.map { p -> p[KEY_LAST_KNOWN_ORG_ID] }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setDefaultNewType(value: String) {
        store.edit { it[KEY_DEFAULT_NEW_TYPE] = value }
    }

    suspend fun setNavCollapsed(value: Boolean) {
        store.edit { it[KEY_NAV_COLLAPSED] = value }
    }

    suspend fun setBaseUrl(value: String) {
        store.edit { it[KEY_BASE_URL] = value }
    }

    suspend fun setLastKnownOrgId(value: Int?) {
        store.edit { p -> if (value == null) p.remove(KEY_LAST_KNOWN_ORG_ID) else p[KEY_LAST_KNOWN_ORG_ID] = value }
    }

    enum class ThemeMode { SYSTEM, LIGHT, DARK }

    companion object {
        const val FILE_NAME = "app_prefs"

        // Default base URL ships from the `bh.baseUrl` Gradle property via
        // BuildConfig. Override at runtime in Settings -> Developer (debug
        // only). Trailing slash is required by Retrofit, so normalize here.
        val BASE_URL_DEFAULT: String = BuildConfig.DEFAULT_BASE_URL.let {
            if (it.endsWith("/")) it else "$it/"
        }

        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DEFAULT_NEW_TYPE = stringPreferencesKey("default_new_type")
        private val KEY_NAV_COLLAPSED = booleanPreferencesKey("nav_collapsed")
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_LAST_KNOWN_ORG_ID = intPreferencesKey("last_known_org_id")
    }
}
