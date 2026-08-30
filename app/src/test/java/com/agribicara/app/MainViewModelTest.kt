package com.agribicara.app

import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import com.agribicara.app.presentation.navigation.Route
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao: UserPreferenceDao = mockk()

    private fun routeOf(viewModel: MainViewModel): String =
        (viewModel.startState.value as StartState.Ready).route

    @Test
    fun `onboarding selesai mengarah ke Home`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { dao.get(Constants.USER_PREFERENCE_ID) } returns
            UserPreferenceEntity(isOnboardingCompleted = true)

        assertEquals(Route.HOME, routeOf(MainViewModel(dao)))
    }

    @Test
    fun `belum onboarding mengarah ke Onboarding`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { dao.get(Constants.USER_PREFERENCE_ID) } returns
            UserPreferenceEntity(isOnboardingCompleted = false)

        assertEquals(Route.ONBOARDING, routeOf(MainViewModel(dao)))
    }

    @Test
    fun `database kosong mengarah ke Onboarding`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { dao.get(Constants.USER_PREFERENCE_ID) } returns null

        assertEquals(Route.ONBOARDING, routeOf(MainViewModel(dao)))
    }

    @Test
    fun `kegagalan baca database tidak menggantung di layar kosong`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        // Regresi: sebelumnya exception di sini membuat startState selamanya
        // Loading, yang dirender sebagai layar putih tanpa jalan keluar.
        coEvery { dao.get(any()) } throws IllegalStateException("DB korup")

        assertEquals(Route.ONBOARDING, routeOf(MainViewModel(dao)))
    }
}
