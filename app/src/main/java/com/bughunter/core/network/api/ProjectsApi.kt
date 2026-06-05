package com.bughunter.core.network.api

import com.bughunter.core.network.dto.ProjectIn
import com.bughunter.core.network.dto.ProjectOut
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface ProjectsApi {

    @GET("api/projects")
    suspend fun list(): List<ProjectOut>

    @POST("api/projects")
    suspend fun create(@Body body: ProjectIn): ProjectOut

    @GET("api/projects/{project_id}")
    suspend fun get(@Path("project_id") projectId: Int): ProjectOut

    @PUT("api/projects/{project_id}")
    suspend fun update(
        @Path("project_id") projectId: Int,
        @Body body: ProjectIn,
    ): ProjectOut

    @DELETE("api/projects/{project_id}")
    suspend fun delete(@Path("project_id") projectId: Int): Map<String, Any?>
}
