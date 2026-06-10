package com.bughunter.core.network.api

import com.bughunter.core.network.dto.DeviceTokenIn
import com.bughunter.core.network.dto.NotificationPreferencesIn
import com.bughunter.core.network.dto.NotificationPreferencesOut
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface DevicesApi {

    @POST("api/devices/register")
    suspend fun register(@Body body: DeviceTokenIn)

    @DELETE("api/devices/{token}")
    suspend fun unregister(@Path("token") token: String)

    @GET("api/notifications/preferences")
    suspend fun preferences(): NotificationPreferencesOut

    @PUT("api/notifications/preferences")
    suspend fun updatePreferences(@Body body: NotificationPreferencesIn): NotificationPreferencesOut
}
