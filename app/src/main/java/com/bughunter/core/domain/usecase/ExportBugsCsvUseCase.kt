package com.bughunter.core.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.bughunter.core.data.repository.BugListFilters
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

internal class ExportBugsCsvUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bugsRepository: BugsRepository,
) {

    data class Export(val uri: Uri, val filename: String)

    suspend operator fun invoke(filters: BugListFilters): Result2<Export> = withContext(Dispatchers.IO) {
        val streamResult = bugsRepository.exportCsvStream(filters)
        if (streamResult is Result2.Err) return@withContext streamResult
        val body = (streamResult as Result2.Ok).value
        val filename = newFilename()
        try {
            body.use { rb -> writeToMediaStore(rb, filename) }
        } catch (t: Throwable) {
            Result2.Err(DomainError.Unknown(t))
        }
    }

    // minSdk 29, so the scoped-storage MediaStore path is the only one that
    // ever runs — IS_PENDING + RELATIVE_PATH lets us write to Downloads without
    // WRITE_EXTERNAL_STORAGE. (Pre-Q legacy file fallback removed; it was dead.)
    private fun writeToMediaStore(body: ResponseBody, filename: String): Result2<Export> {
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, MIME_CSV)
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/$SUBDIR")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values)
            ?: return Result2.Err(DomainError.Server("Failed to allocate Downloads entry"))
        try {
            resolver.openOutputStream(uri)?.use { out ->
                body.byteStream().use { input -> input.copyTo(out) }
            } ?: return Result2.Err(DomainError.Server("Failed to open Downloads stream"))
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            return Result2.Err(DomainError.Unknown(t))
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return Result2.Ok(Export(uri = uri, filename = filename))
    }

    private fun newFilename(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "bugs-export-$stamp.csv"
    }

    companion object {
        private const val MIME_CSV = "text/csv"
        private const val SUBDIR = "BugHunter"
    }
}
