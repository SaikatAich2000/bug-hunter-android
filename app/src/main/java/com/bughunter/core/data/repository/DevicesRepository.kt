package com.bughunter.core.data.repository

import com.bughunter.core.data.local.PushPrefs
import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.DevicesApi
import com.bughunter.core.network.dto.DeviceTokenIn
import com.bughunter.core.network.dto.NotificationPreferencesIn
import com.bughunter.core.network.dto.NotificationPreferencesOut
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around DevicesApi + PushPrefs.
 *
 *   - registerIfChanged() — POST only when the local cache is out of
 *     date. Keeps cold-start launches free of an unneeded network call.
 *
 *   - forceRegister() — POST unconditionally. Called from FCM's
 *     onNewToken() callback because that's the only moment we KNOW the
 *     token actually changed (Firebase rotates it on app re-install,
 *     data-clear, GCM unregister, etc.).
 *
 *   - unregister() — DELETE the row server-side and clear the local
 *     cache. Called on logout so the now-signed-out device stops
 *     receiving the previous user's pushes.
 */
@Singleton
internal class DevicesRepository @Inject constructor(
    private val api: DevicesApi,
    private val errorMapper: ErrorMapper,
    private val pushPrefs: PushPrefs,
) {

    suspend fun registerIfChanged(token: String): Result2<Unit> {
        val last = pushPrefs.lastRegisteredToken.firstOrNull()
        if (last == token) return Result2.Ok(Unit)
        return forceRegister(token)
    }

    suspend fun forceRegister(token: String): Result2<Unit> {
        val result = runResult(errorMapper) { api.register(DeviceTokenIn(token = token)) }
        if (result is Result2.Ok) pushPrefs.setLastRegisteredToken(token)
        return result
    }

    suspend fun unregister(token: String): Result2<Unit> {
        val result = runResult(errorMapper) { api.unregister(token) }
        // Clear local cache regardless of server result — on logout we
        // want the next user (or re-login) to re-register from scratch.
        pushPrefs.setLastRegisteredToken(null)
        return result
    }

    suspend fun preferences(): Result2<NotificationPreferencesOut> =
        runResult(errorMapper) { api.preferences() }

    suspend fun updatePreferences(body: NotificationPreferencesIn): Result2<NotificationPreferencesOut> =
        runResult(errorMapper) { api.updatePreferences(body) }
}
