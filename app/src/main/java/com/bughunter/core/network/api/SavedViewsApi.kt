package com.bughunter.core.network.api

import com.bughunter.core.network.dto.SavedViewIn
import com.bughunter.core.network.dto.SavedViewOut
import com.bughunter.core.network.dto.SavedViewUpdateIn
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface SavedViewsApi {

    @GET("api/saved-views")
    suspend fun list(): List<SavedViewOut>

    @POST("api/saved-views")
    suspend fun create(@Body body: SavedViewIn): SavedViewOut

    @GET("api/saved-views/{view_id}")
    suspend fun get(@Path("view_id") viewId: Int): SavedViewOut

    @PUT("api/saved-views/{view_id}")
    suspend fun update(
        @Path("view_id") viewId: Int,
        @Body body: SavedViewUpdateIn,
    ): SavedViewOut

    @DELETE("api/saved-views/{view_id}")
    suspend fun delete(@Path("view_id") viewId: Int)
}
