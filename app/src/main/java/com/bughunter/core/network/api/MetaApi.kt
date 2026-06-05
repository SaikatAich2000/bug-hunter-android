package com.bughunter.core.network.api

import com.bughunter.core.network.dto.HealthOut
import com.bughunter.core.network.dto.MetaOut
import retrofit2.http.GET

internal interface MetaApi {

    @GET("api/health")
    suspend fun health(): HealthOut

    @GET("api/meta")
    suspend fun meta(): MetaOut
}
