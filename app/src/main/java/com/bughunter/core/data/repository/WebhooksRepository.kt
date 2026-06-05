package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.WebhooksApi
import com.bughunter.core.network.dto.WebhookIn
import com.bughunter.core.network.dto.WebhookOut
import com.bughunter.core.network.dto.WebhookUpdateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WebhooksRepository @Inject constructor(
    private val api: WebhooksApi,
    private val errorMapper: ErrorMapper,
) {
    suspend fun list(): Result2<List<WebhookOut>> = runResult(errorMapper) { api.list() }

    suspend fun create(body: WebhookIn): Result2<WebhookOut> =
        runResult(errorMapper) { api.create(body) }

    suspend fun get(hookId: Int): Result2<WebhookOut> =
        runResult(errorMapper) { api.get(hookId) }

    suspend fun update(hookId: Int, body: WebhookUpdateIn): Result2<WebhookOut> =
        runResult(errorMapper) { api.update(hookId, body) }

    suspend fun delete(hookId: Int): Result2<Unit> =
        runResult(errorMapper) { api.delete(hookId) }

    suspend fun test(hookId: Int): Result2<Map<String, String>> =
        runResult(errorMapper) { api.test(hookId) }
}
