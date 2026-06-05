package com.bughunter.core.network.api

import com.bughunter.core.network.dto.WebhookIn
import com.bughunter.core.network.dto.WebhookOut
import com.bughunter.core.network.dto.WebhookUpdateIn
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface WebhooksApi {

    @GET("api/webhooks")
    suspend fun list(): List<WebhookOut>

    @POST("api/webhooks")
    suspend fun create(@Body body: WebhookIn): WebhookOut

    @GET("api/webhooks/{hook_id}")
    suspend fun get(@Path("hook_id") hookId: Int): WebhookOut

    @PUT("api/webhooks/{hook_id}")
    suspend fun update(
        @Path("hook_id") hookId: Int,
        @Body body: WebhookUpdateIn,
    ): WebhookOut

    @DELETE("api/webhooks/{hook_id}")
    suspend fun delete(@Path("hook_id") hookId: Int)

    @POST("api/webhooks/{hook_id}/test")
    suspend fun test(@Path("hook_id") hookId: Int): Map<String, String>
}
