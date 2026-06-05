package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.MetaApi
import com.bughunter.core.network.dto.HealthOut
import com.bughunter.core.network.dto.MetaOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

internal data class MetaSnapshot(
    val meta: MetaOut,
    val fetchedAtEpochMs: Long,
)

@Singleton
internal class MetaRepository @Inject constructor(
    private val api: MetaApi,
    private val errorMapper: ErrorMapper,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val mutex = Mutex()
    private val _snapshot = MutableStateFlow<MetaSnapshot?>(null)
    val snapshot: StateFlow<MetaSnapshot?> = _snapshot.asStateFlow()

    suspend fun health(): Result2<HealthOut> = runResult(errorMapper) { api.health() }

    suspend fun meta(forceRefresh: Boolean = false): Result2<MetaOut> = mutex.withLock {
        val cached = _snapshot.value
        if (!forceRefresh && cached != null && isFresh(cached)) {
            return@withLock Result2.Ok(cached.meta)
        }
        val result = runResult(errorMapper) { api.meta() }
        if (result is Result2.Ok) {
            _snapshot.value = MetaSnapshot(meta = result.value, fetchedAtEpochMs = clock())
        }
        result
    }

    fun clear() {
        _snapshot.value = null
    }

    private fun isFresh(snapshot: MetaSnapshot): Boolean =
        clock() - snapshot.fetchedAtEpochMs < CACHE_TTL.toMillis()

    companion object {
        val CACHE_TTL: Duration = Duration.ofHours(1)
    }
}
