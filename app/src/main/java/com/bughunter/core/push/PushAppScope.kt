package com.bughunter.core.push

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Process-lifetime coroutine scope for fire-and-forget background work
 * like FCM token register / unregister.
 *
 * Why this exists: the auth flow used to AWAIT
 * `pushTokenSyncer.registerCurrent()` inside `AuthRepository.onAuthenticated`.
 * That made a failing FCM device-register block the entire sign-in /
 * sign-up coroutine — when the backend was unreachable, the button
 * spun for the full HTTP timeout instead of completing immediately.
 *
 * SupervisorJob so a crash in one push task doesn't cancel siblings.
 * Dispatchers.IO so the work doesn't compete with the UI thread.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class PushScope

@Module
@InstallIn(SingletonComponent::class)
internal object PushAppScopeModule {

    @Provides
    @Singleton
    @PushScope
    fun providePushScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
