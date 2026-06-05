package com.bughunter.core.ui.util

import com.bughunter.core.network.DomainError

internal sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: DomainError) : UiState<Nothing>
}

internal inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Success -> UiState.Success(transform(data))
    is UiState.Error -> this
    UiState.Loading -> UiState.Loading
    UiState.Empty -> UiState.Empty
}

internal fun <T> UiState<T>.dataOrNull(): T? = (this as? UiState.Success)?.data

internal val UiState<*>.isLoading: Boolean get() = this is UiState.Loading
