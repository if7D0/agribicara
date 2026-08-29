package com.agribicara.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Mengganti Dispatchers.Main untuk unit test.
 *
 * Dispatcher-nya di-expose supaya test bisa memakai scheduler YANG SAMA di
 * dalam runTest. Membuat UnconfinedTestDispatcher() tanpa argumen akan
 * membangun scheduler terpisah yang tidak dikendalikan runTest — assertion
 * bisa berjalan sebelum coroutine ViewModel selesai dan test lolos/gagal
 * secara kebetulan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}
