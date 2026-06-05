package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.ProjectsApi
import com.bughunter.core.network.dto.ProjectIn
import com.bughunter.core.network.dto.ProjectOut
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ProjectsRepository @Inject constructor(
    private val api: ProjectsApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(): Result2<List<ProjectOut>> = runResult(errorMapper) { api.list() }

    suspend fun create(body: ProjectIn): Result2<ProjectOut> =
        runResult(errorMapper) { api.create(body) }

    suspend fun get(projectId: Int): Result2<ProjectOut> =
        runResult(errorMapper) { api.get(projectId) }

    suspend fun update(projectId: Int, body: ProjectIn): Result2<ProjectOut> =
        runResult(errorMapper) { api.update(projectId, body) }

    suspend fun delete(projectId: Int): Result2<Map<String, Any?>> =
        runResult(errorMapper) { api.delete(projectId) }
}
