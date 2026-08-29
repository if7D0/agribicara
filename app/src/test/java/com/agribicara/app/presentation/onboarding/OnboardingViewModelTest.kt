package com.agribicara.app.presentation.onboarding

import com.agribicara.app.MainDispatcherRule
import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.dao.UserPreferenceDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao: UserPreferenceDao = mockk(relaxed = true)

    @Test
    fun `completeOnboarding menandai selesai lewat operasi atomik`() = runTest {
        val viewModel = OnboardingViewModel(dao)

        viewModel.completeOnboarding()

        coVerify(exactly = 1) { dao.markOnboardingCompleted(Constants.USER_PREFERENCE_ID) }
        assertTrue(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `tap ganda hanya menulis sekali`() = runTest {
        // Penulisan sengaja dibuat menggantung agar tap kedua tiba saat isSaving true.
        coEvery { dao.markOnboardingCompleted(any()) } coAnswers { delay(50) }
        val viewModel = OnboardingViewModel(dao)

        viewModel.completeOnboarding()
        viewModel.completeOnboarding()
        advanceUntilIdleCompat()

        coVerify(exactly = 1) { dao.markOnboardingCompleted(any()) }
        assertTrue(viewModel.uiState.value.isCompleted)
    }

    @Test
    fun `kegagalan penulisan memunculkan pesan error dan tidak menandai selesai`() = runTest {
        coEvery { dao.markOnboardingCompleted(any()) } throws IllegalStateException("disk full")
        val viewModel = OnboardingViewModel(dao)

        viewModel.completeOnboarding()

        assertFalse(viewModel.uiState.value.isCompleted)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Gagal menyimpan. Coba lagi.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `onErrorShown membersihkan pesan error`() = runTest {
        coEvery { dao.markOnboardingCompleted(any()) } throws IllegalStateException("boom")
        val viewModel = OnboardingViewModel(dao)
        viewModel.completeOnboarding()

        viewModel.onErrorShown()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    private suspend fun advanceUntilIdleCompat() = delay(200)
}
