package com.agribicara.app.presentation.onboarding

import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val dao: UserPreferenceDao = mockk(relaxed = true)
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        // viewModelScope memakai Dispatchers.Main -> harus diganti di unit test
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = OnboardingViewModel(dao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `completeOnboarding menyimpan flag lalu memanggil callback`() = runTest {
        // Arrange
        coEvery { dao.get(Constants.USER_PREFERENCE_ID) } returns null
        val saved = slot<UserPreferenceEntity>()
        var callbackCalled = false

        // Act
        viewModel.completeOnboarding { callbackCalled = true }

        // Assert
        coVerify(exactly = 1) { dao.upsert(capture(saved)) }
        assertTrue(saved.captured.isOnboardingCompleted)
        assertEquals(Constants.USER_PREFERENCE_ID, saved.captured.id)
        assertTrue(callbackCalled)
    }

    @Test
    fun `completeOnboarding mempertahankan region yang sudah tersimpan`() = runTest {
        // Arrange: pengguna sudah punya region (skenario Fase 2), jangan sampai hilang
        coEvery { dao.get(Constants.USER_PREFERENCE_ID) } returns UserPreferenceEntity(
            isOnboardingCompleted = false,
            regionCode = "31.71.01.1001",
            regionName = "Kelurahan Contoh",
        )
        val saved = slot<UserPreferenceEntity>()

        // Act
        viewModel.completeOnboarding { }

        // Assert
        coVerify(exactly = 1) { dao.upsert(capture(saved)) }
        assertTrue(saved.captured.isOnboardingCompleted)
        assertEquals("31.71.01.1001", saved.captured.regionCode)
        assertEquals("Kelurahan Contoh", saved.captured.regionName)
    }
}
