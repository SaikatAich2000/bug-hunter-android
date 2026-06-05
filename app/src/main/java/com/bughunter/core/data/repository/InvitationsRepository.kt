package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.InvitationsApi
import com.bughunter.core.network.dto.InvitationAccept
import com.bughunter.core.network.dto.InvitationCreate
import com.bughunter.core.network.dto.InvitationOut
import com.bughunter.core.network.dto.InvitationPreview
import com.bughunter.core.network.dto.MeOut
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InvitationsRepository @Inject constructor(
    private val api: InvitationsApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(): Result2<List<InvitationOut>> = runResult(errorMapper) { api.list() }

    suspend fun create(body: InvitationCreate): Result2<InvitationOut> =
        runResult(errorMapper) { api.create(body) }

    suspend fun delete(invitationId: Int): Result2<Map<String, Any?>> =
        runResult(errorMapper) { api.delete(invitationId) }

    suspend fun preview(token: String): Result2<InvitationPreview> =
        runResult(errorMapper) { api.preview(token) }

    suspend fun accept(body: InvitationAccept): Result2<MeOut> =
        runResult(errorMapper) { api.accept(body) }
}
