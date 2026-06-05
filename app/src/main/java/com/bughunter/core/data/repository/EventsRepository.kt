package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.EventsApi
import com.bughunter.core.network.dto.EventCreate
import com.bughunter.core.network.dto.EventDetailOut
import com.bughunter.core.network.dto.EventOut
import com.bughunter.core.network.dto.EventUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class EventsRepository @Inject constructor(
    private val api: EventsApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(scheduledFor: String? = null): Result2<List<EventOut>> =
        runResult(errorMapper) { api.list(scheduledFor) }

    suspend fun create(body: EventCreate): Result2<EventOut> =
        runResult(errorMapper) { api.create(body) }

    suspend fun get(eventId: Int): Result2<EventDetailOut> =
        runResult(errorMapper) { api.get(eventId) }

    suspend fun update(eventId: Int, body: EventUpdate): Result2<EventOut> =
        runResult(errorMapper) { api.update(eventId, body) }

    suspend fun delete(eventId: Int): Result2<Map<String, String>> =
        runResult(errorMapper) { api.delete(eventId) }
}
