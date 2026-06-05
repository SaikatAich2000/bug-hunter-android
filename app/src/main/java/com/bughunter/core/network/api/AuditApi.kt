package com.bughunter.core.network.api

import com.bughunter.core.network.dto.ActivityOut
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Streaming

internal interface AuditApi {

    @GET("api/audit")
    suspend fun list(
        @Query("entity_type") entityType: String? = null,
        @Query("actor_user_id") actorUserId: Int? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 5000,
        @Query("offset") offset: Int = 0,
    ): List<ActivityOut>

    @Streaming
    @Headers("Accept: text/csv")
    @GET("api/audit/export.csv")
    suspend fun exportCsv(
        @Query("entity_type") entityType: String? = null,
        @Query("actor_user_id") actorUserId: Int? = null,
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 10000,
    ): ResponseBody
}
