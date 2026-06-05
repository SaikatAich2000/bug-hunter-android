package com.bughunter.core.network.api

import com.bughunter.core.network.dto.CustomFieldIn
import com.bughunter.core.network.dto.CustomFieldOut
import com.bughunter.core.network.dto.CustomFieldUpdateIn
import com.bughunter.core.network.dto.CustomValueOut
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

internal interface CustomFieldsApi {

    @GET("api/projects/{project_id}/custom-fields")
    suspend fun listForProject(@Path("project_id") projectId: Int): List<CustomFieldOut>

    @POST("api/projects/{project_id}/custom-fields")
    suspend fun create(
        @Path("project_id") projectId: Int,
        @Body body: CustomFieldIn,
    ): CustomFieldOut

    @PUT("api/projects/{project_id}/custom-fields/{field_id}")
    suspend fun update(
        @Path("project_id") projectId: Int,
        @Path("field_id") fieldId: Int,
        @Body body: CustomFieldUpdateIn,
    ): CustomFieldOut

    @DELETE("api/projects/{project_id}/custom-fields/{field_id}")
    suspend fun delete(
        @Path("project_id") projectId: Int,
        @Path("field_id") fieldId: Int,
    )

    @GET("api/bugs/{bug_id}/custom-values")
    suspend fun listValuesForBug(@Path("bug_id") bugId: Int): List<CustomValueOut>

    @PUT("api/bugs/{bug_id}/custom-values")
    suspend fun setValuesForBug(
        @Path("bug_id") bugId: Int,
        @Body values: List<CustomValueOut>,
    ): List<CustomValueOut>
}
