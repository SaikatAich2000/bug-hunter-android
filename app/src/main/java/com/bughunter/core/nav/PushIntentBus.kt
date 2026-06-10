package com.bughunter.core.nav

import android.content.Intent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Tiny pub/sub for `Intent`s that should be routed through the Compose
 * navigation controller. MainActivity publishes; BhAppShell collects and
 * forwards to NavController.handleDeepLink().
 *
 * Why a process-singleton object instead of a Hilt-scoped flow:
 *   - MainActivity is created by the system, not by Hilt, so we can't
 *     inject a flow into onCreate / onNewIntent without a clunky
 *     EntryPoint lookup.
 *   - The bus has no per-user state; a `replay = 1` SharedFlow lets a
 *     late-subscribing nav controller still receive the cold-start
 *     intent that arrived before it was set up.
 *
 * BufferOverflow.DROP_OLDEST so a rapid back-to-back tap (two pushes,
 * second tap before the first is consumed) doesn't deadlock.
 */
internal object PushIntentBus {

    private val _intents = MutableSharedFlow<Intent>(
        replay = 1,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val intents: SharedFlow<Intent> = _intents.asSharedFlow()

    fun publish(intent: Intent) {
        _intents.tryEmit(intent)
    }
}
