package com.bughunter.core.data.repository

import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2

internal suspend inline fun <T> runResult(
    errorMapper: ErrorMapper,
    crossinline block: suspend () -> T,
): Result2<T> = try {
    Result2.Ok(block())
} catch (t: Throwable) {
    Result2.Err(errorMapper.map(t))
}
