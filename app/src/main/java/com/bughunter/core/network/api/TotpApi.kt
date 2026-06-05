package com.bughunter.core.network.api

import com.bughunter.core.network.dto.TotpBeginOut
import com.bughunter.core.network.dto.TotpConfirmIn
import com.bughunter.core.network.dto.TotpConfirmOut
import com.bughunter.core.network.dto.TotpDisableIn
import com.bughunter.core.network.dto.TotpStatus
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

internal interface TotpApi {

    @GET("api/auth/2fa/status")
    suspend fun status(): TotpStatus

    @POST("api/auth/2fa/begin")
    suspend fun begin(): TotpBeginOut

    @POST("api/auth/2fa/confirm")
    suspend fun confirm(@Body body: TotpConfirmIn): TotpConfirmOut

    @POST("api/auth/2fa/disable")
    suspend fun disable(@Body body: TotpDisableIn)

    @POST("api/auth/2fa/recovery-codes/regenerate")
    suspend fun regenerateRecoveryCodes(): TotpConfirmOut
}
