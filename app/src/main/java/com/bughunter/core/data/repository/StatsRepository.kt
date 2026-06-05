package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.StatsApi
import com.bughunter.core.network.dto.StatsOut
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StatsRepository @Inject constructor(
    private val api: StatsApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun get(itemType: String? = null): Result2<StatsOut> =
        runResult(errorMapper) { api.get(itemType) }
}
