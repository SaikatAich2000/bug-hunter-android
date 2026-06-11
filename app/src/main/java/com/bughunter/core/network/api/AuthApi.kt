package com.bughunter.core.network.api

import com.bughunter.core.network.dto.ChangePasswordIn
import com.bughunter.core.network.dto.DeleteAccountIn
import com.bughunter.core.network.dto.EmailChangeConfirmIn
import com.bughunter.core.network.dto.EmailChangeRequestIn
import com.bughunter.core.network.dto.ForgotPasswordIn
import com.bughunter.core.network.dto.LoginIn
import com.bughunter.core.network.dto.LoginResponse
import com.bughunter.core.network.dto.LoginTotpStepIn
import com.bughunter.core.network.dto.MeOut
import com.bughunter.core.network.dto.ProfileUpdateIn
import com.bughunter.core.network.dto.ResetPasswordIn
import com.bughunter.core.network.dto.SignupIn
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT

internal interface AuthApi {

    @POST("api/auth/signup")
    suspend fun signup(@Body body: SignupIn): MeOut

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginIn): LoginResponse

    @POST("api/auth/login/totp")
    suspend fun loginTotp(@Body body: LoginTotpStepIn): MeOut

    @POST("api/auth/logout")
    suspend fun logout()

    @GET("api/auth/me")
    suspend fun me(): MeOut

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordIn)

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordIn)

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordIn)

    @PUT("api/auth/profile")
    suspend fun updateProfile(@Body body: ProfileUpdateIn): MeOut

    @POST("api/auth/email-change/request")
    suspend fun requestEmailChange(@Body body: EmailChangeRequestIn): Map<String, String>

    @POST("api/auth/email-change/confirm")
    suspend fun confirmEmailChange(@Body body: EmailChangeConfirmIn): MeOut

    @GET("api/auth/data-export")
    suspend fun dataExport(): Map<String, Any?>

    // DELETE with body — Retrofit requires @HTTP with hasBody=true.
    @HTTP(method = "DELETE", path = "api/auth/account", hasBody = true)
    suspend fun deleteAccount(@Body body: DeleteAccountIn)
}
