package com.bughunter.core.network.api

import com.bughunter.core.network.dto.ChatIn
import com.bughunter.core.network.dto.ChatOut
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

internal interface ChatApi {

    @POST("api/chat/ask")
    suspend fun ask(@Body body: ChatIn): ChatOut

    @Streaming
    @GET("api/chat/download/{token}")
    suspend fun download(@Path("token") token: String): ResponseBody
}
