package com.bughunter.core.push

import android.content.Context
import com.bughunter.core.data.local.PushPrefs
import com.bughunter.core.data.repository.DevicesRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Asks Firebase for the current FCM token and pushes it to the server
 * if it doesn't match what we last registered.
 *
 * Called from two places:
 *  1. AuthRepository.onAuthenticated() — right after login, so the
 *     server knows which user owns this device.
 *  2. AuthRepository.clearLocalSession() — passes the cached token to
 *     the unregister job so the server stops pushing to a now-signed-
 *     out device. We do NOT delete the Firebase token itself; doing so
 *     would force a full token regenerate next time the app runs, which
 *     is more expensive than just letting the new login re-register.
 *
 * Firebase's getToken() is a Task<String>; we wrap it as a suspend
 * function so the auth flow can await it without callback gymnastics.
 *
 * Tests inject [PushTokenSync.Noop] via the AuthRepository constructor —
 * the interface keeps Firebase out of the unit-test classpath.
 */
internal interface PushTokenSync {
    suspend fun registerCurrent()
    suspend fun unregisterCurrent()

    /** Inert implementation for unit tests that don't care about push. */
    object Noop : PushTokenSync {
        override suspend fun registerCurrent() = Unit
        override suspend fun unregisterCurrent() = Unit
    }
}

@Singleton
internal class PushTokenSyncer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val devices: DevicesRepository,
    private val pushPrefs: PushPrefs,
) : PushTokenSync {

    override suspend fun registerCurrent() {
        val token = currentToken() ?: return
        // We use the lighter registerIfChanged() (no-op when the cache
        // says "we already pushed this token"). onNewToken() always
        // forceRegister() because that's the moment we KNOW it changed.
        devices.registerIfChanged(token)
    }

    override suspend fun unregisterCurrent() {
        // Prefer the cached token. If the cache was already cleared
        // (e.g. logout called twice) fall back to whatever Firebase
        // currently holds — better an extra DELETE than a leaked row.
        val token = pushPrefs.lastRegisteredToken.firstOrNull() ?: currentToken() ?: return
        DeviceRegistrationWorker.enqueueUnregister(context, token)
    }

    private suspend fun currentToken(): String? = try {
        suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    } catch (_: Throwable) {
        // Token fetch can fail on emulators without Play Services or on
        // devices in Doze. Swallow — a later cold start will retry.
        null
    }
}

/** Binds the concrete syncer to the interface so Hilt can resolve the
 *  AuthRepository constructor dependency without a manual @Provides. */
@Module
@InstallIn(SingletonComponent::class)
internal interface PushTokenSyncModule {
    @Binds
    fun bind(impl: PushTokenSyncer): PushTokenSync
}
