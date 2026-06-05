package com.bughunter.core.network.api

import com.bughunter.core.network.dto.SessionOut
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

internal interface SessionsApi {

    @GET("api/sessions")
    suspend fun list(): List<SessionOut>

    @DELETE("api/sessions/{session_id}")
    suspend fun revoke(@Path("session_id") sessionId: Int): Map<String, String>
}
