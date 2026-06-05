package com.bughunter.core.network.api

import com.bughunter.core.network.dto.UserIn
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.network.dto.UserUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface UsersApi {

    @GET("api/users")
    suspend fun list(
        @Query("include_inactive") includeInactive: Boolean = true,
        @Query("q") query: String? = null,
    ): List<UserOut>

    @POST("api/users")
    suspend fun create(@Body body: UserIn): UserOut

    @GET("api/users/{user_id}")
    suspend fun get(@Path("user_id") userId: Int): UserOut

    @PUT("api/users/{user_id}")
    suspend fun update(
        @Path("user_id") userId: Int,
        @Body body: UserUpdate,
    ): UserOut

    @DELETE("api/users/{user_id}")
    suspend fun delete(@Path("user_id") userId: Int): Map<String, String>
}
