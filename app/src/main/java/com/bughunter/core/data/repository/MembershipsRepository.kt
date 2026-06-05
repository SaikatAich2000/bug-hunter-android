package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.MembershipsApi
import com.bughunter.core.network.dto.ProjectMembershipIn
import com.bughunter.core.network.dto.ProjectMembershipOut
import com.bughunter.core.network.dto.ProjectMembershipUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MembershipsRepository @Inject constructor(
    private val api: MembershipsApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(projectId: Int): Result2<List<ProjectMembershipOut>> =
        runResult(errorMapper) { api.list(projectId) }

    suspend fun add(projectId: Int, body: ProjectMembershipIn): Result2<ProjectMembershipOut> =
        runResult(errorMapper) { api.add(projectId, body) }

    suspend fun update(
        projectId: Int,
        userId: Int,
        body: ProjectMembershipUpdate,
    ): Result2<ProjectMembershipOut> = runResult(errorMapper) { api.update(projectId, userId, body) }

    suspend fun remove(projectId: Int, userId: Int): Result2<Map<String, Any?>> =
        runResult(errorMapper) { api.remove(projectId, userId) }
}
