package com.agribicara.app.presentation.home

import com.agribicara.app.MainDispatcherRule
import com.agribicara.app.R
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.NotificationPromptStore
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.domain.usecase.GetForecastUseCase
import com.agribicara.app.domain.usecase.ObserveSelectedRegionUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    /**
     * Penanda izin notifikasi (F6 L5) untuk test yang tidak mempedulikannya.
     *
     * Test yang MEMANG memeriksa jalur ini membuat store-nya sendiri lewat
     * [storePenanda], karena mereka perlu mengatur emisi dan kegagalan.
     */
    private val notificationPromptStore = storePenanda(flowOf(false))

    /**
     * Store penanda izin dengan emisi yang ditentukan pemanggil.
     *
     * [gagalMenyimpan] mensimulasikan `markAsked()` yang melempar — berkas
     * rusak atau penyimpanan penuh.
     */
    private fun storePenanda(
        emisi: Flow<Boolean>,
        gagalMenyimpan: Throwable? = null,
    ) = mockk<NotificationPromptStore>(relaxed = true).apply {
        every { hasAsked } returns emisi
        if (gagalMenyimpan != null) {
            coEvery { markAsked() } throws gagalMenyimpan
        }
    }

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
        val viewModel = HomeViewModel(clockAt(16), getForecast, observeSelectedRegion, notificationPromptStore)
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

        val viewModel = HomeViewModel(movingClock, getForecast, observeSelectedRegion, notificationPromptStore)
        assertEquals(R.string.home_greeting_morning, viewModel.uiState.value.greetingRes)

        now = Instant.parse("2026-08-29T12:00:00Z") // 19:00 WIB
        viewModel.refreshGreeting()

        assertEquals(R.string.home_greeting_night, viewModel.uiState.value.greetingRes)
    }

    @Test
    fun `cuaca hari ini terisi saat wilayah sudah dipilih`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        withRegionAndForecast()

        val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion, notificationPromptStore)

        val state = viewModel.uiState.value
        assertEquals("Cibeureum", state.regionName)
        assertEquals("Cerah Berawan", state.today?.description)
        assertTrue(!state.needsRegion)
    }

    @Test
    fun `needsRegion true bila pengguna belum memilih wilayah`() = runTest(mainDispatcherRule.testDispatcher.scheduler) {
        every { observeSelectedRegion() } returns flowOf(null)

        val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion, notificationPromptStore)

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

        val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion, notificationPromptStore)

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

        val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion, notificationPromptStore)

        assertTrue(viewModel.uiState.value.isOffline)
    }

    // --- Penanda izin notifikasi (F6 L5) -----------------------------------
    //
    // Sebelum tinjauan Fase 9, SELURUH perbaikan F6 L5 tidak punya satu pun
    // test: store-nya hanya di-mock agar konstruktor kompilasi. Mengembalikan
    // `notificationPromptAsked` menjadi `Boolean = false` — persis regresi yang
    // diperingatkan KDoc-nya — tetap meloloskan seluruh suite. Pengecualian
    // Kover untuk NotificationPromptStore juga disandarkan pada penjaga ini
    // ("yang berperilaku adalah HomeViewModel, dan itu diuji"), jadi sampai
    // test-test di bawah ada, pembenaran itu belum benar.

    @Test
    fun `penanda izin bernilai null selama penyimpanan belum menjawab`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            withRegionAndForecast()

            // emptyFlow: store tersambung tetapi belum mengemisikan apa pun.
            val viewModel = HomeViewModel(
                clockAt(10), getForecast, observeSelectedRegion, storePenanda(emptyFlow()),
            )

            // null, BUKAN false. Layar memakai perbedaan ini untuk menahan diri
            // tidak meminta izin sebelum jawabannya tiba; menyamakan keduanya
            // memunculkan dialog sekejap — kambuhnya F6 L5 dalam bentuk lain.
            assertNull(viewModel.uiState.value.notificationPromptAsked)
        }

    @Test
    fun `penanda izin mengikuti nilai yang tersimpan`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            withRegionAndForecast()
            val penanda = MutableStateFlow(false)

            val viewModel = HomeViewModel(
                clockAt(10), getForecast, observeSelectedRegion, storePenanda(penanda),
            )
            assertFalse(viewModel.uiState.value.notificationPromptAsked!!)

            // Perubahan di penyimpanan sampai ke state tanpa layar dibangun
            // ulang — itulah beda DataStore dengan rememberSaveable yang diganti.
            penanda.value = true
            assertTrue(viewModel.uiState.value.notificationPromptAsked!!)
        }

    @Test
    fun `menampilkan dialog izin menyimpan penandanya`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            withRegionAndForecast()
            val store = storePenanda(flowOf(false))
            val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion, store)

            viewModel.onNotificationPromptShown()

            // Tanpa ini penanda tidak pernah tersimpan dan dialog kembali
            // muncul pada entri navigasi berikutnya — temuan F6 L5 itu sendiri.
            coVerify(exactly = 1) { store.markAsked() }
        }

    @Test
    fun `kegagalan menyimpan penanda tidak menjatuhkan aplikasi`() =
        runTest(mainDispatcherRule.testDispatcher.scheduler) {
            withRegionAndForecast()
            val store = storePenanda(flowOf(false), gagalMenyimpan = IOException("penyimpanan penuh"))
            val viewModel = HomeViewModel(clockAt(10), getForecast, observeSelectedRegion, store)

            // Gagal menyimpan berarti dialog bisa muncul sekali lagi nanti —
            // mengganggu, bukan merusak. Test ini gagal dengan melempar, jadi
            // tidak perlu assert: yang diuji adalah TIDAK adanya lemparan.
            viewModel.onNotificationPromptShown()

            assertFalse(viewModel.uiState.value.notificationPromptAsked!!)
        }
}
