package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.AuditApi
import com.bughunter.core.network.dto.ActivityOut
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AuditRepository @Inject constructor(
    private val api: AuditApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(
        entityType: String? = null,
        actorUserId: Int? = null,
        query: String? = null,
        limit: Int = 5000,
        offset: Int = 0,
    ): Result2<List<ActivityOut>> = runResult(errorMapper) {
        api.list(entityType, actorUserId, query, limit, offset)
    }

    suspend fun exportCsvStream(
        entityType: String? = null,
        actorUserId: Int? = null,
        query: String? = null,
        limit: Int = 10000,
    ): Result2<ResponseBody> = runResult(errorMapper) {
        api.exportCsv(entityType, actorUserId, query, limit)
    }
}
