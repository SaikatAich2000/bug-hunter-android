package com.bughunter.core.network.api

import com.bughunter.core.network.dto.ProjectMembershipIn
import com.bughunter.core.network.dto.ProjectMembershipOut
import com.bughunter.core.network.dto.ProjectMembershipUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface MembershipsApi {

    @GET("api/projects/{project_id}/members")
    suspend fun list(@Path("project_id") projectId: Int): List<ProjectMembershipOut>

    @POST("api/projects/{project_id}/members")
    suspend fun add(
        @Path("project_id") projectId: Int,
        @Body body: ProjectMembershipIn,
    ): ProjectMembershipOut

    @PUT("api/projects/{project_id}/members/{user_id}")
    suspend fun update(
        @Path("project_id") projectId: Int,
        @Path("user_id") userId: Int,
        @Body body: ProjectMembershipUpdate,
    ): ProjectMembershipOut

    @DELETE("api/projects/{project_id}/members/{user_id}")
    suspend fun remove(
        @Path("project_id") projectId: Int,
        @Path("user_id") userId: Int,
    ): Map<String, Any?>
}
