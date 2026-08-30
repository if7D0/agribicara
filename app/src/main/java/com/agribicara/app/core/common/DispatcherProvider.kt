package com.agribicara.app.core.common

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatcher yang bisa di-inject.
 *
 * Jangan memakai [Dispatchers] secara langsung di repository/use case — inject
 * antarmuka ini supaya unit test bisa menyuntikkan test dispatcher dan berjalan
 * deterministik.
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

class DefaultDispatcherProvider @Inject constructor() : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val main: CoroutineDispatcher = Dispatchers.Main
}
