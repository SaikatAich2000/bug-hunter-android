package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.BugsApi
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BugsRepository @Inject constructor(
    private val api: BugsApi,
    private val errorMapper: ErrorMapper,
) {

    suspend fun list(filters: BugListFilters): Result2<BugListResponse> = runResult(errorMapper) {
        api.list(
            projectId = filters.projectIdsOrNull(),
            status = filters.statusesOrNull(),
            priority = filters.prioritiesOrNull(),
            environment = filters.environmentsOrNull(),
            reporterId = filters.reporterId,
            assigneeId = filters.assigneeIdsOrNull(),
            itemType = filters.itemTypesOrNull(),
            eventId = filters.eventId,
            query = filters.query,
            page = filters.page,
            pageSize = filters.pageSize,
        )
    }

    suspend fun create(body: BugCreate): Result2<BugOut> = runResult(errorMapper) { api.create(body) }

    suspend fun get(bugId: Int): Result2<BugDetail> = runResult(errorMapper) { api.get(bugId) }

    suspend fun update(bugId: Int, body: BugUpdate): Result2<BugOut> =
        runResult(errorMapper) { api.update(bugId, body) }

    suspend fun delete(bugId: Int): Result2<Map<String, String>> =
        runResult(errorMapper) { api.delete(bugId) }

    suspend fun bulkUpdate(body: BulkUpdateIn): Result2<Map<String, Any?>> =
        runResult(errorMapper) { api.bulkUpdate(body) }

    suspend fun bulkDelete(body: BulkDeleteIn): Result2<Map<String, Any?>> =
        runResult(errorMapper) { api.bulkDelete(body) }

    suspend fun exportCsvStream(filters: BugListFilters): Result2<ResponseBody> = runResult(errorMapper) {
        api.exportCsv(
            projectId = filters.projectIdsOrNull(),
            status = filters.statusesOrNull(),
            priority = filters.prioritiesOrNull(),
            environment = filters.environmentsOrNull(),
            reporterId = filters.reporterId,
            assigneeId = filters.assigneeIdsOrNull(),
            itemType = filters.itemTypesOrNull(),
            eventId = filters.eventId,
            query = filters.query,
        )
    }

    suspend fun listComments(bugId: Int): Result2<List<CommentOut>> =
        runResult(errorMapper) { api.listComments(bugId) }

    suspend fun addComment(bugId: Int, body: CommentIn): Result2<CommentOut> =
        runResult(errorMapper) { api.addComment(bugId, body) }

    suspend fun updateComment(bugId: Int, commentId: Int, body: CommentIn): Result2<CommentOut> =
        runResult(errorMapper) { api.updateComment(bugId, commentId, body) }

    suspend fun deleteComment(bugId: Int, commentId: Int): Result2<Unit> =
        runResult(errorMapper) { api.deleteComment(bugId, commentId) }

    suspend fun uploadAttachment(
        bugId: Int,
        filePart: MultipartBody.Part,
        commentId: RequestBody? = null,
    ): Result2<AttachmentBrief> = runResult(errorMapper) {
        api.uploadAttachment(bugId, filePart, commentId)
    }

    suspend fun downloadAttachment(bugId: Int, attachmentId: Int): Result2<ResponseBody> =
        runResult(errorMapper) { api.downloadAttachment(bugId, attachmentId) }

    suspend fun deleteAttachment(bugId: Int, attachmentId: Int): Result2<Map<String, Any?>> =
        runResult(errorMapper) { api.deleteAttachment(bugId, attachmentId) }

    suspend fun activity(bugId: Int): Result2<List<ActivityOut>> =
        runResult(errorMapper) { api.activity(bugId) }
}
