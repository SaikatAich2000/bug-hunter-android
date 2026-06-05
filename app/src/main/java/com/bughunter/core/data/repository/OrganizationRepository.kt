package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.OrganizationApi
import com.bughunter.core.network.dto.OrganizationOut
import com.bughunter.core.network.dto.OrganizationUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OrganizationRepository @Inject constructor(
    private val api: OrganizationApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun get(): Result2<OrganizationOut> = runResult(errorMapper) { api.get() }

    suspend fun update(body: OrganizationUpdate): Result2<OrganizationOut> =
        runResult(errorMapper) { api.update(body) }
}
