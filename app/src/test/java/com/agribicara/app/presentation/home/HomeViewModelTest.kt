package com.agribicara.app.presentation.home

import com.agribicara.app.MainDispatcherRule
import com.agribicara.app.R
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getForecast = mockk<GetForecastUseCase>()
    private val observeSelectedRegion = mockk<ObserveSelectedRegionUseCase>()

    private fun clockAt(hour: Int, minute: Int = 0): Clock {
        val zone = ZoneId.of("Asia/Jakarta")
        val instant = LocalDate.of(2026, 8, 29)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
        return Clock.fixed(instant, zone)
    }

    private fun sampleForecast() = Forecast(
        regionCode = "32.77.01.1002",
        regionName = "Cibeureum",
        days = listOf(
            DailyForecast(
                date = LocalDate.of(2026, 8, 29),
                temperatureMax = 30.0,
                temperatureMin = 20.0,
                precipitationMm = 0.0,
                windSpeed = 3.6,
                weatherCode = 2,
                description = "Cerah Berawan",
                source = WeatherSource.BMKG,
            ),
        ),
        source = WeatherSource.BMKG,
        fetchedAt = 0L,
    )

    /** Wilayah sudah dipilih dan cuaca tersedia — kondisi normal. */
    private fun withRegionAndForecast() {
        every { observeSelectedRegion() } returns flowOf(
            Region("32.77.01.1002", "Cibeureum", RegionLevel.VILLAGE),
        )
        coEvery { getForecast(any(), any()) } returns NetworkResult.Success(sampleForecast())
    }

    @Test
    fun `sapaan mengikuti batas jam yang benar`() {
        assertEquals(R.string.home_greeting_morning, HomeViewModel.greetingFor(LocalTime.of(4, 0)))
        assertEquals(R.string.home_greeting_morning, HomeViewModel.greetingFor(LocalTime.of(10, 59)))
        assertEquals(R.string.home_greeting_afternoon, HomeViewModel.greetingFor(LocalTime.of(11, 0)))
        assertEquals(R.string.home_greeting_afternoon, HomeViewModel.greetingFor(LocalTime.of(14, 59)))
        assertEquals(R.string.home_greeting_evening, HomeViewModel.greetingFor(LocalTime.of(15, 0)))
        assertEquals(R.string.home_greeting_evening, HomeViewModel.greetingFor(LocalTime.of(18, 59)))
        assertEquals(R.string.home_greeting_night, HomeViewModel.greetingFor(LocalTime.of(19, 0)))
        assertEquals(R.string.home_greeting_night, HomeViewModel.greetingFor(LocalTime.of(3, 59)))
    }

    @Test
    fun `state awal memakai jam yang disuntikkan`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        withRegionAndForecast()
        val viewModel = HomeViewModel(clockAt(16), getForecast, observeSelectedRegion)
        assertEquals(R.string.home_greeting_evening, viewModel.uiState.value.greetingRes)
    }

    @Test
    fun `refreshGreeting memperbarui sapaan yang sudah basi`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        withRegionAndForecast()
        // Waktu maju dari pagi ke malam selagi ViewModel tetap hidup.
        var now = Instant.parse("2026-08-29T02:00:00Z") // 09:00 WIB
        val zone = ZoneId.of("Asia/Jakarta")
        val movingClock = object : Clock() {
            override fun getZone(): ZoneId = zone
            override fun withZone(z: ZoneId): Clock = this
            override fun instant(): Instant = now
        }

        val viewModel = HomeViewModel(movingClock, getForecast, observeSelectedRegion)
        assertEquals(R.string.home_greeting_morning, viewModel.uiState.value.greetingRes)

        now = Instant.parse("2026-08-29T12:00:00Z") // 19:00 WIB
        viewModel.refreshGreeting()

        assertEquals(R.string.home_greeting_night, viewModel.uiState.value.greetingRes)
    }

    @Test
    fun `cuaca hari ini terisi saat wilayah sudah dipilih`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        withRegionAndForecast()

        val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion)

        val state = viewModel.uiState.value
        assertEquals("Cibeureum", state.regionName)
        assertEquals("Cerah Berawan", state.today?.description)
        assertTrue(!state.needsRegion)
    }

    @Test
    fun `needsRegion true bila pengguna belum memilih wilayah`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        every { observeSelectedRegion() } returns flowOf(null)

        val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion)

        assertTrue(viewModel.uiState.value.needsRegion)
        assertNull(viewModel.uiState.value.today)
    }

    @Test
    fun `kegagalan cuaca tidak memblokir Home`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        // Tombol mic harus tetap bisa dipakai walau cuaca gagal dimuat —
        // itu fitur inti produk, cuaca hanya pendukung.
        every { observeSelectedRegion() } returns flowOf(
            Region("32.77.01.1002", "Cibeureum", RegionLevel.VILLAGE),
        )
        coEvery { getForecast(any(), any()) } returns NetworkResult.Error("gagal")

        val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion)

        val state = viewModel.uiState.value
        assertNull(state.today)
        assertTrue(!state.isWeatherLoading)
        assertTrue(!state.needsRegion)
    }

    @Test
    fun `banner offline aktif saat data berasal dari cache`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        every { observeSelectedRegion() } returns flowOf(
            Region("32.77.01.1002", "Cibeureum", RegionLevel.VILLAGE),
        )
        coEvery { getForecast(any(), any()) } returns NetworkResult.Success(
            sampleForecast().copy(source = WeatherSource.CACHE),
        )

        val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion)

        assertTrue(viewModel.uiState.value.isOffline)
    }
}
