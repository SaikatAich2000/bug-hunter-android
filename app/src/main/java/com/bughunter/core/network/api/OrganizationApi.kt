package com.bughunter.core.network.api

import com.bughunter.core.network.dto.OrganizationOut
import com.bughunter.core.network.dto.OrganizationUpdate
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

internal interface OrganizationApi {

    @GET("api/organization")
    suspend fun get(): OrganizationOut

    @PUT("api/organization")
    suspend fun update(@Body body: OrganizationUpdate): OrganizationOut
}
