package com.agribicara.app.core.common

/**
 * Tipe hasil kanonik untuk seluruh operasi yang bisa gagal.
 *
 * Aturan (berlaku untuk semua fase):
 * - Setiap repository mengembalikan [NetworkResult], tidak pernah melempar exception
 *   melewati batas repository.
 * - [Error.message] selalu Bahasa Indonesia sederhana karena langsung tampil ke petani.
 * - [Error.cause] hanya untuk logging, jangan pernah ditampilkan ke user.
 */
sealed interface NetworkResult<out T> {

    data class Success<T>(val data: T) : NetworkResult<T>

    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : NetworkResult<Nothing>

    data object Loading : NetworkResult<Nothing>
}

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data))
    is NetworkResult.Error -> this
    NetworkResult.Loading -> NetworkResult.Loading
}

fun <T> NetworkResult<T>.dataOrNull(): T? = (this as? NetworkResult.Success)?.data
