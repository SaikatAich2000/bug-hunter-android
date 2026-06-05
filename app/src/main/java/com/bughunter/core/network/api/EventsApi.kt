package com.bughunter.core.network.api

import com.bughunter.core.network.dto.EventCreate
import com.bughunter.core.network.dto.EventDetailOut
import com.bughunter.core.network.dto.EventOut
import com.bughunter.core.network.dto.EventUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface EventsApi {

    @GET("api/events")
    suspend fun list(
        @Query("scheduled_for") scheduledFor: String? = null,
    ): List<EventOut>

    @POST("api/events")
    suspend fun create(@Body body: EventCreate): EventOut

    @GET("api/events/{event_id}")
    suspend fun get(@Path("event_id") eventId: Int): EventDetailOut

    @PUT("api/events/{event_id}")
    suspend fun update(
        @Path("event_id") eventId: Int,
        @Body body: EventUpdate,
    ): EventOut

    @DELETE("api/events/{event_id}")
    suspend fun delete(@Path("event_id") eventId: Int): Map<String, String>
}
