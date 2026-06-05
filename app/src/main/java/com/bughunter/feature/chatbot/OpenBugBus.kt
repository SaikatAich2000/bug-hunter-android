package com.bughunter.feature.chatbot

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OpenBugBus @Inject constructor() {

    private val _events = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val events: SharedFlow<Int> = _events.asSharedFlow()

    suspend fun emit(bugId: Int) {
        _events.emit(bugId)
    }

    fun tryEmit(bugId: Int): Boolean = _events.tryEmit(bugId)
}
