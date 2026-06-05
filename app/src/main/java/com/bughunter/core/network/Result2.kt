package com.bughunter.core.network

sealed class Result2<out T> {
    data class Ok<T>(val value: T) : Result2<T>()
    data class Err(val error: DomainError) : Result2<Nothing>()
}

inline fun <T, R> Result2<T>.map(transform: (T) -> R): Result2<R> = when (this) {
    is Result2.Ok -> Result2.Ok(transform(value))
    is Result2.Err -> this
}

inline fun <T, R> Result2<T>.flatMap(transform: (T) -> Result2<R>): Result2<R> = when (this) {
    is Result2.Ok -> transform(value)
    is Result2.Err -> this
}

inline fun <T> Result2<T>.onOk(block: (T) -> Unit): Result2<T> {
    if (this is Result2.Ok) block(value)
    return this
}

inline fun <T> Result2<T>.onErr(block: (DomainError) -> Unit): Result2<T> {
    if (this is Result2.Err) block(error)
    return this
}

fun <T> Result2<T>.getOrNull(): T? = (this as? Result2.Ok)?.value
fun <T> Result2<T>.errorOrNull(): DomainError? = (this as? Result2.Err)?.error
