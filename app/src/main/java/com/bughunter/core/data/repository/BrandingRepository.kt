package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.BrandingApi
import com.bughunter.core.network.dto.BrandingIn
import com.bughunter.core.network.dto.BrandingOut
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BrandingRepository @Inject constructor(
    private val api: BrandingApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun get(): Result2<BrandingOut> = runResult(errorMapper) { api.get() }

    suspend fun update(body: BrandingIn): Result2<BrandingOut> =
        runResult(errorMapper) { api.update(body) }
}
