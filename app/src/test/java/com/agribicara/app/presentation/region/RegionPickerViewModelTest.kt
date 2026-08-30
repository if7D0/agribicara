package com.agribicara.app.presentation.region

import com.agribicara.app.MainDispatcherRule
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.domain.usecase.GetRegionsUseCase
import com.agribicara.app.domain.usecase.SaveSelectedRegionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegionPickerViewModelTest {

    // StandardTestDispatcher (bukan Unconfined) supaya urutan penyelesaian
    // coroutine bisa dikendalikan — dibutuhkan test respons kadaluwarsa.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val getRegions = mockk<GetRegionsUseCase>()
    private val saveSelectedRegion = mockk<SaveSelectedRegionUseCase>(relaxed = true)

    private val jabar = Region("32", "Jawa Barat", RegionLevel.PROVINCE)
    private val jateng = Region("33", "Jawa Tengah", RegionLevel.PROVINCE)
    private val cimahi = Region("32.77", "Kota Cimahi", RegionLevel.REGENCY)
    private val cimahiSelatan = Region("32.77.01", "Cimahi Selatan", RegionLevel.DISTRICT)
    private val cibeureum = Region("32.77.01.1002", "Cibeureum", RegionLevel.VILLAGE)

    private fun viewModel() = RegionPickerViewModel(getRegions, saveSelectedRegion)

    @Test
    fun `provinsi dimuat saat inisialisasi`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        coEvery { getRegions(RegionLevel.PROVINCE, null, any()) } returns
            NetworkResult.Success(listOf(jabar, jateng))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(RegionLevel.PROVINCE, vm.uiState.value.level)
        assertEquals(2, vm.uiState.value.items.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `memilih provinsi turun ke kabupaten dan mengisi breadcrumb`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { getRegions(RegionLevel.PROVINCE, null, any()) } returns
                NetworkResult.Success(listOf(jabar))
            coEvery { getRegions(RegionLevel.REGENCY, "32", any()) } returns
                NetworkResult.Success(listOf(cimahi))

            val vm = viewModel()
            advanceUntilIdle()
            vm.onSelect(jabar)
            advanceUntilIdle()

            assertEquals(RegionLevel.REGENCY, vm.uiState.value.level)
            assertEquals(listOf("Jawa Barat"), vm.uiState.value.breadcrumb.map { it.name })
            assertEquals(listOf("Kota Cimahi"), vm.uiState.value.items.map { it.name })
        }

    @Test
    fun `memilih kelurahan menyimpan dan menandai selesai`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { getRegions(any(), any(), any()) } returns NetworkResult.Success(emptyList())

            val vm = viewModel()
            advanceUntilIdle()
            vm.onSelect(cibeureum)
            advanceUntilIdle()

            coVerify { saveSelectedRegion(cibeureum) }
            assertTrue(vm.uiState.value.isCompleted)
        }

    @Test
    fun `back naik satu tingkat dan memangkas breadcrumb`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { getRegions(RegionLevel.PROVINCE, null, any()) } returns
                NetworkResult.Success(listOf(jabar))
            coEvery { getRegions(RegionLevel.REGENCY, "32", any()) } returns
                NetworkResult.Success(listOf(cimahi))
            coEvery { getRegions(RegionLevel.DISTRICT, "32.77", any()) } returns
                NetworkResult.Success(listOf(cimahiSelatan))

            val vm = viewModel()
            advanceUntilIdle()
            vm.onSelect(jabar); advanceUntilIdle()
            vm.onSelect(cimahi); advanceUntilIdle()
            assertEquals(RegionLevel.DISTRICT, vm.uiState.value.level)

            val handled = vm.onBack()
            advanceUntilIdle()

            assertTrue(handled)
            assertEquals(RegionLevel.REGENCY, vm.uiState.value.level)
            assertEquals(listOf("Jawa Barat"), vm.uiState.value.breadcrumb.map { it.name })
        }

    @Test
    fun `back di provinsi mengembalikan false supaya layar bisa ditutup`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { getRegions(any(), any(), any()) } returns
                NetworkResult.Success(listOf(jabar))

            val vm = viewModel()
            advanceUntilIdle()

            assertFalse(vm.onBack())
        }

    @Test
    fun `pencarian memfilter tanpa mengubah daftar sumber`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { getRegions(any(), any(), any()) } returns
                NetworkResult.Success(listOf(jabar, jateng))

            val vm = viewModel()
            advanceUntilIdle()
            vm.onQueryChange("tengah")

            assertEquals(listOf("Jawa Tengah"), vm.uiState.value.visibleItems.map { it.name })
            assertEquals(2, vm.uiState.value.items.size)
        }

    @Test
    fun `pencarian tidak peduli huruf besar kecil`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { getRegions(any(), any(), any()) } returns
                NetworkResult.Success(listOf(jabar, jateng))

            val vm = viewModel()
            advanceUntilIdle()
            vm.onQueryChange("BARAT")

            assertEquals(listOf("Jawa Barat"), vm.uiState.value.visibleItems.map { it.name })
        }

    @Test
    fun `retry memaksa refresh melewati cache`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            // Tanpa forceRefresh, cache yang terpotong tidak akan pernah pulih.
            coEvery { getRegions(any(), any(), any()) } returns
                NetworkResult.Success(listOf(jabar))

            val vm = viewModel()
            advanceUntilIdle()
            vm.retry()
            advanceUntilIdle()

            coVerify { getRegions(RegionLevel.PROVINCE, null, true) }
        }

    @Test
    fun `respons tingkat yang sudah ditinggalkan tidak menimpa daftar terkini`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { getRegions(RegionLevel.PROVINCE, null, any()) } returns
                NetworkResult.Success(listOf(jabar, jateng))
            // Kabupaten Jawa Barat LAMBAT, Jawa Tengah CEPAT.
            coEvery { getRegions(RegionLevel.REGENCY, "32", any()) } coAnswers {
                delay(1_000)
                NetworkResult.Success(listOf(cimahi))
            }
            coEvery { getRegions(RegionLevel.REGENCY, "33", any()) } returns
                NetworkResult.Success(listOf(Region("33.74", "Kota Semarang", RegionLevel.REGENCY)))

            val vm = viewModel()
            advanceUntilIdle()

            vm.onSelect(jabar)     // mulai request lambat
            vm.onBack()            // pengguna kembali sebelum selesai
            advanceUntilIdle()
            vm.onSelect(jateng)    // pilih provinsi lain, request cepat
            advanceUntilIdle()

            // Respons Jawa Barat harus sudah dibatalkan, bukan mendarat
            // belakangan dan menimpa daftar Jawa Tengah.
            assertEquals(listOf("Kota Semarang"), vm.uiState.value.items.map { it.name })
            assertEquals(listOf("Jawa Tengah"), vm.uiState.value.breadcrumb.map { it.name })
        }

    @Test
    fun `pesan error masuk ke state dan bisa dibersihkan`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            coEvery { getRegions(any(), any(), any()) } returns
                NetworkResult.Error("Daftar wilayah gagal dimuat.")

            val vm = viewModel()
            advanceUntilIdle()

            assertEquals("Daftar wilayah gagal dimuat.", vm.uiState.value.errorMessage)
            assertFalse(vm.uiState.value.isLoading)

            vm.onErrorShown()
            assertNull(vm.uiState.value.errorMessage)
        }
}
