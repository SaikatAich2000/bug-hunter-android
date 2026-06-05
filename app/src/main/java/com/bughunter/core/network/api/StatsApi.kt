package com.bughunter.core.network.api

import com.bughunter.core.network.dto.StatsOut
import retrofit2.http.GET
import retrofit2.http.Query

internal interface StatsApi {

    @GET("api/stats")
    suspend fun get(
        @Query("item_type") itemType: String? = null,
    ): StatsOut
}
