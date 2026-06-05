package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.SavedViewsApi
import com.bughunter.core.network.dto.SavedViewIn
import com.bughunter.core.network.dto.SavedViewOut
import com.bughunter.core.network.dto.SavedViewUpdateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SavedViewsRepository @Inject constructor(
    private val api: SavedViewsApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(): Result2<List<SavedViewOut>> = runResult(errorMapper) { api.list() }

    suspend fun create(body: SavedViewIn): Result2<SavedViewOut> =
        runResult(errorMapper) { api.create(body) }

    suspend fun get(viewId: Int): Result2<SavedViewOut> =
        runResult(errorMapper) { api.get(viewId) }

    suspend fun update(viewId: Int, body: SavedViewUpdateIn): Result2<SavedViewOut> =
        runResult(errorMapper) { api.update(viewId, body) }

    suspend fun delete(viewId: Int): Result2<Unit> =
        runResult(errorMapper) { api.delete(viewId) }
}
