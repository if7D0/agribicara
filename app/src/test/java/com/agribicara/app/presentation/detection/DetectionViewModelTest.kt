package com.agribicara.app.presentation.detection

import android.net.Uri
import com.agribicara.app.MainDispatcherRule
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.ConfidenceBand
import com.agribicara.app.domain.model.DetectionOutcome
import com.agribicara.app.domain.model.DetectionOutcomeType
import com.agribicara.app.domain.model.DiseaseDetection
import com.agribicara.app.domain.repository.DiseaseRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<DiseaseRepository>()
    private val uri = mockk<Uri>()

    private fun viewModel(): DetectionViewModel {
        every { repository.observeHistory(any()) } returns flowOf(emptyList())
        return DetectionViewModel(repository)
    }

    @Test
    fun `analyze sukses menaruh outcome dan mematikan loading`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { repository.classify(any()) } returns
                NetworkResult.Success(DetectionOutcome.Healthy(0.9f, ConfidenceBand.STRONG))

            val vm = viewModel()
            vm.analyze(uri)

            val state = vm.uiState.value
            assertFalse(state.isAnalyzing)
            assertTrue(state.outcome is DetectionOutcome.Healthy)
            assertNull(state.errorMessage)
        }

    @Test
    fun `analyze gagal menaruh pesan error`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { repository.classify(any()) } returns NetworkResult.Error("model belum ada")

            val vm = viewModel()
            vm.analyze(uri)

            val state = vm.uiState.value
            assertFalse(state.isAnalyzing)
            assertNull(state.outcome)
            assertEquals("model belum ada", state.errorMessage)
        }

    @Test
    fun `reset mengosongkan hasil untuk foto ulang`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { repository.classify(any()) } returns
                NetworkResult.Success(DetectionOutcome.Unsure(0.3f))

            val vm = viewModel()
            vm.analyze(uri)
            assertTrue(vm.uiState.value.outcome is DetectionOutcome.Unsure)

            vm.reset()
            assertNull(vm.uiState.value.outcome)
        }

    @Test
    fun `riwayat dari repository muncul di state`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            every { repository.observeHistory(any()) } returns flowOf(
                listOf(DiseaseDetection(1, DetectionOutcomeType.HEALTHY, null, 0.9f, 1L)),
            )

            val vm = DetectionViewModel(repository)

            assertEquals(1, vm.uiState.value.history.size)
        }
}
