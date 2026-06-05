package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.UsersApi
import com.bughunter.core.network.dto.UserIn
import com.bughunter.core.network.dto.UserOut
import com.bughunter.core.network.dto.UserUpdate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UsersRepository @Inject constructor(
    private val api: UsersApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(
        includeInactive: Boolean = true,
        query: String? = null,
    ): Result2<List<UserOut>> = runResult(errorMapper) { api.list(includeInactive, query) }

    suspend fun create(body: UserIn): Result2<UserOut> = runResult(errorMapper) { api.create(body) }

    suspend fun get(userId: Int): Result2<UserOut> = runResult(errorMapper) { api.get(userId) }

    suspend fun update(userId: Int, body: UserUpdate): Result2<UserOut> =
        runResult(errorMapper) { api.update(userId, body) }

    suspend fun delete(userId: Int): Result2<Map<String, String>> =
        runResult(errorMapper) { api.delete(userId) }
}
