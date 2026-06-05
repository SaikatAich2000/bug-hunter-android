package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.CustomFieldsApi
import com.bughunter.core.network.dto.CustomFieldIn
import com.bughunter.core.network.dto.CustomFieldOut
import com.bughunter.core.network.dto.CustomFieldUpdateIn
import com.bughunter.core.network.dto.CustomValueOut
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class CustomFieldsRepository @Inject constructor(
    private val api: CustomFieldsApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun listForProject(projectId: Int): Result2<List<CustomFieldOut>> =
        runResult(errorMapper) { api.listForProject(projectId) }

    suspend fun create(projectId: Int, body: CustomFieldIn): Result2<CustomFieldOut> =
        runResult(errorMapper) { api.create(projectId, body) }

    suspend fun update(
        projectId: Int,
        fieldId: Int,
        body: CustomFieldUpdateIn,
    ): Result2<CustomFieldOut> = runResult(errorMapper) { api.update(projectId, fieldId, body) }

    suspend fun delete(projectId: Int, fieldId: Int): Result2<Unit> =
        runResult(errorMapper) { api.delete(projectId, fieldId) }

    suspend fun listValuesForBug(bugId: Int): Result2<List<CustomValueOut>> =
        runResult(errorMapper) { api.listValuesForBug(bugId) }

    suspend fun setValuesForBug(
        bugId: Int,
        values: List<CustomValueOut>,
    ): Result2<List<CustomValueOut>> =
        runResult(errorMapper) { api.setValuesForBug(bugId, values) }
}
