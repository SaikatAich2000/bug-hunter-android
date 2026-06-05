package com.bughunter.core.network.api

import com.bughunter.core.network.dto.ActivityOut
import com.bughunter.core.network.dto.AttachmentBrief
import com.bughunter.core.network.dto.BugCreate
import com.bughunter.core.network.dto.BugDetail
import com.bughunter.core.network.dto.BugListResponse
import com.bughunter.core.network.dto.BugOut
import com.bughunter.core.network.dto.BugUpdate
import com.bughunter.core.network.dto.BulkDeleteIn
import com.bughunter.core.network.dto.BulkUpdateIn
import com.bughunter.core.network.dto.CommentIn
import com.bughunter.core.network.dto.CommentOut
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

internal interface BugsApi {

    @GET("api/bugs")
    suspend fun list(
        @Query("project_id") projectId: List<Int>? = null,
        @Query("status") status: List<String>? = null,
        @Query("priority") priority: List<String>? = null,
        @Query("environment") environment: List<String>? = null,
        @Query("reporter_id") reporterId: Int? = null,
        @Query("assignee_id") assigneeId: List<Int>? = null,
        @Query("item_type") itemType: List<String>? = null,
        @Query("event_id") eventId: Int? = null,
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): BugListResponse

    @POST("api/bugs")
    suspend fun create(@Body body: BugCreate): BugOut

    @GET("api/bugs/{bug_id}")
    suspend fun get(@Path("bug_id") bugId: Int): BugDetail

    @PUT("api/bugs/{bug_id}")
    suspend fun update(
        @Path("bug_id") bugId: Int,
        @Body body: BugUpdate,
    ): BugOut

    @DELETE("api/bugs/{bug_id}")
    suspend fun delete(@Path("bug_id") bugId: Int): Map<String, String>

    @POST("api/bugs/bulk-update")
    suspend fun bulkUpdate(@Body body: BulkUpdateIn): Map<String, Any?>

    @POST("api/bugs/bulk-delete")
    suspend fun bulkDelete(@Body body: BulkDeleteIn): Map<String, Any?>

    @Streaming
    @Headers("Accept: text/csv")
    @GET("api/bugs/export.csv")
    suspend fun exportCsv(
        @Query("project_id") projectId: List<Int>? = null,
        @Query("status") status: List<String>? = null,
        @Query("priority") priority: List<String>? = null,
        @Query("environment") environment: List<String>? = null,
        @Query("reporter_id") reporterId: Int? = null,
        @Query("assignee_id") assigneeId: List<Int>? = null,
        @Query("item_type") itemType: List<String>? = null,
        @Query("event_id") eventId: Int? = null,
        @Query("q") query: String? = null,
    ): ResponseBody

    @GET("api/bugs/{bug_id}/comments")
    suspend fun listComments(@Path("bug_id") bugId: Int): List<CommentOut>

    @POST("api/bugs/{bug_id}/comments")
    suspend fun addComment(
        @Path("bug_id") bugId: Int,
        @Body body: CommentIn,
    ): CommentOut

    @PUT("api/bugs/{bug_id}/comments/{comment_id}")
    suspend fun updateComment(
        @Path("bug_id") bugId: Int,
        @Path("comment_id") commentId: Int,
        @Body body: CommentIn,
    ): CommentOut

    @DELETE("api/bugs/{bug_id}/comments/{comment_id}")
    suspend fun deleteComment(
        @Path("bug_id") bugId: Int,
        @Path("comment_id") commentId: Int,
    )

    @Multipart
    @POST("api/bugs/{bug_id}/attachments")
    suspend fun uploadAttachment(
        @Path("bug_id") bugId: Int,
        @Part file: MultipartBody.Part,
        @Part("comment_id") commentId: RequestBody? = null,
    ): AttachmentBrief

    @Streaming
    @GET("api/bugs/{bug_id}/attachments/{att_id}/download")
    suspend fun downloadAttachment(
        @Path("bug_id") bugId: Int,
        @Path("att_id") attachmentId: Int,
    ): ResponseBody

    @DELETE("api/bugs/{bug_id}/attachments/{att_id}")
    suspend fun deleteAttachment(
        @Path("bug_id") bugId: Int,
        @Path("att_id") attachmentId: Int,
    ): Map<String, Any?>

    @GET("api/bugs/{bug_id}/activity")
    suspend fun activity(@Path("bug_id") bugId: Int): List<ActivityOut>
}
