package com.bughunter.core.network.api

import com.bughunter.core.network.dto.InvitationAccept
import com.bughunter.core.network.dto.InvitationCreate
import com.bughunter.core.network.dto.InvitationOut
import com.bughunter.core.network.dto.InvitationPreview
import com.bughunter.core.network.dto.MeOut
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

internal interface InvitationsApi {

    @GET("api/invitations")
    suspend fun list(): List<InvitationOut>

    @POST("api/invitations")
    suspend fun create(@Body body: InvitationCreate): InvitationOut

    @DELETE("api/invitations/{invitation_id}")
    suspend fun delete(@Path("invitation_id") invitationId: Int): Map<String, Any?>

    @GET("api/invitations/preview/{token}")
    suspend fun preview(@Path("token") token: String): InvitationPreview

    @POST("api/invitations/accept")
    suspend fun accept(@Body body: InvitationAccept): MeOut
}
