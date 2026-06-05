package com.bughunter.core.network.api

import com.bughunter.core.network.dto.BrandingIn
import com.bughunter.core.network.dto.BrandingOut
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

internal interface BrandingApi {

    @GET("api/branding")
    suspend fun get(): BrandingOut

    @PUT("api/branding")
    suspend fun update(@Body body: BrandingIn): BrandingOut
}
