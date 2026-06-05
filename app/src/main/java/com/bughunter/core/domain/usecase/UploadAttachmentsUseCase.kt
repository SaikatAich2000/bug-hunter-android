package com.bughunter.core.domain.usecase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.AttachmentBrief
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import javax.inject.Inject

internal class UploadAttachmentsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bugsRepository: BugsRepository,
) {

    sealed interface UploadOutcome {
        val uri: Uri

        data class Uploaded(override val uri: Uri, val attachment: AttachmentBrief) : UploadOutcome
        data class Skipped(override val uri: Uri, val reason: String) : UploadOutcome
        data class Failed(override val uri: Uri, val error: DomainError) : UploadOutcome
    }

    suspend operator fun invoke(
        bugId: Int,
        uris: List<Uri>,
        commentId: Int? = null,
    ): List<UploadOutcome> = withContext(Dispatchers.IO) {
        val commentIdBody: RequestBody? = commentId?.toString()?.toRequestBody(TEXT_PLAIN)
        uris.map { uri -> uploadOne(bugId, uri, commentIdBody) }
    }

    private suspend fun uploadOne(
        bugId: Int,
        uri: Uri,
        commentIdBody: RequestBody?,
    ): UploadOutcome {
        val filename = resolveFilename(uri) ?: "attachment.bin"
        if (isBlocked(filename)) {
            return UploadOutcome.Skipped(uri, "Blocked unsafe file: $filename")
        }

        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: IOException) {
            null
        } ?: return UploadOutcome.Failed(uri, DomainError.Network)

        val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", filename, body)

        return when (val r = bugsRepository.uploadAttachment(bugId, part, commentIdBody)) {
            is Result2.Ok -> UploadOutcome.Uploaded(uri, r.value)
            is Result2.Err -> UploadOutcome.Failed(uri, r.error)
        }
    }

    private fun resolveFilename(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx)
            }
        }
        return uri.lastPathSegment
    }

    internal fun isBlocked(filename: String): Boolean {
        val lower = filename.lowercase()
        return BLOCKED_EXTENSIONS.any { lower.endsWith(it) }
    }

    companion object {
        private val TEXT_PLAIN = "text/plain".toMediaTypeOrNull()
        internal val BLOCKED_EXTENSIONS: Set<String> = setOf(
            ".exe", ".bat", ".sh", ".lnk", ".cmd", ".com", ".scr",
            ".ps1", ".vbs", ".js", ".jar", ".msi", ".dll", ".reg",
        )
    }
}
