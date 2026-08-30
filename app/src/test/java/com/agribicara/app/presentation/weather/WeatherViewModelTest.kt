package com.agribicara.app.presentation.weather

import com.agribicara.app.MainDispatcherRule
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.domain.usecase.GetForecastUseCase
import com.agribicara.app.domain.usecase.ObserveSelectedRegionUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
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
class WeatherViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getForecast = mockk<GetForecastUseCase>()
    private val observeSelectedRegion = mockk<ObserveSelectedRegionUseCase>()

    private val selectedRegion = Region("32.77.01.1002", "Cibeureum", RegionLevel.VILLAGE)

    private fun day(offset: Long, source: WeatherSource) = DailyForecast(
        date = LocalDate.of(2026, 8, 29).plusDays(offset),
        temperatureMax = 30.0,
        temperatureMin = 20.0,
        precipitationMm = 0.0,
        windSpeed = 3.6,
        weatherCode = 1,
        description = if (source == WeatherSource.BMKG) "Cerah" else null,
        source = source,
    )

    @Test
    fun `tujuh hari tampil dengan tiga hari pertama dari BMKG`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        every { observeSelectedRegion() } returns flowOf(selectedRegion)
        coEvery { getForecast(any(), any()) } returns NetworkResult.Success(
            Forecast(
                regionCode = selectedRegion.code,
                regionName = selectedRegion.name,
                days = (0..2).map { day(it.toLong(), WeatherSource.BMKG) } +
                    (3..6).map { day(it.toLong(), WeatherSource.OPEN_METEO) },
                source = WeatherSource.BMKG,
                fetchedAt = 0L,
            ),
        )

        val viewModel = WeatherViewModel(getForecast, observeSelectedRegion)

        val state = viewModel.uiState.value
        assertEquals(7, state.days.size)
        assertEquals(WeatherSource.BMKG, state.days[0].source)
        assertEquals(WeatherSource.OPEN_METEO, state.days[3].source)
        assertFalse(state.isOffline)
    }

    @Test
    fun `isOffline true hanya saat sumbernya cache`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        every { observeSelectedRegion() } returns flowOf(selectedRegion)
        coEvery { getForecast(any(), any()) } returns NetworkResult.Success(
            Forecast(
                regionCode = selectedRegion.code,
                regionName = selectedRegion.name,
                days = listOf(day(0, WeatherSource.CACHE)),
                source = WeatherSource.CACHE,
                fetchedAt = 0L,
            ),
        )

        val viewModel = WeatherViewModel(getForecast, observeSelectedRegion)

        assertTrue(viewModel.uiState.value.isOffline)
    }

    @Test
    fun `needsRegion true bila wilayah belum dipilih`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        every { observeSelectedRegion() } returns flowOf(null)

        val viewModel = WeatherViewModel(getForecast, observeSelectedRegion)

        assertTrue(viewModel.uiState.value.needsRegion)
        assertTrue(viewModel.uiState.value.days.isEmpty())
    }

    @Test
    fun `pesan error diteruskan ke state dan bisa dibersihkan`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        every { observeSelectedRegion() } returns flowOf(selectedRegion)
        coEvery { getForecast(any(), any()) } returns NetworkResult.Error("Tidak ada koneksi.")

        val viewModel = WeatherViewModel(getForecast, observeSelectedRegion)
        assertEquals("Tidak ada koneksi.", viewModel.uiState.value.errorMessage)

        // Tanpa pembersihan, snackbar akan muncul lagi setiap rekomposisi.
        viewModel.onErrorShown()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `nama wilayah tetap tampil walau pengambilan gagal`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        every { observeSelectedRegion() } returns flowOf(selectedRegion)
        coEvery { getForecast(any(), any()) } returns NetworkResult.Error("gagal")

        val viewModel = WeatherViewModel(getForecast, observeSelectedRegion)

        assertEquals("Cibeureum", viewModel.uiState.value.regionName)
    }
}
