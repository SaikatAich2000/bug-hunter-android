package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.SessionsApi
import com.bughunter.core.network.dto.SessionOut
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SessionsRepository @Inject constructor(
    private val api: SessionsApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(): Result2<List<SessionOut>> = runResult(errorMapper) { api.list() }

    suspend fun revoke(sessionId: Int): Result2<Map<String, String>> =
        runResult(errorMapper) { api.revoke(sessionId) }
}
