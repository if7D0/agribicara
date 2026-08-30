package com.agribicara.app.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.notification.WeatherNotifier
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.HourlyForecast
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.model.RegionLevel
import com.agribicara.app.domain.model.WeatherAlert
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.domain.repository.RegionRepository
import com.agribicara.app.domain.repository.WeatherRepository
import com.agribicara.app.domain.usecase.GetForecastUseCase
import com.agribicara.app.domain.usecase.ObserveSelectedRegionUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Alur worker pemeriksa cuaca.
 *
 * Instrumented, bukan unit test seperti yang diperkirakan plan: [CoroutineWorker]
 * menuntut Context dan WorkerParameters yang sungguhan, dan project ini tidak
 * memakai Robolectric. `TestListenableWorkerBuilder` menjalankan worker-nya
 * langsung tanpa menunggu penjadwalan.
 *
 * Tiga keputusan di dalam `doWork` diuji, dan ketiganya punya konsekuensi nyata:
 * belum ada wilayah bukan kegagalan, kegagalan jaringan harus retry bukan
 * failure, dan cuaca aman tidak boleh mengganggu siapa pun.
 */
@RunWith(AndroidJUnit4::class)
class WeatherCheckWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val keudeBakongan = Region("11.01.01.2001", "Keude Bakongan", RegionLevel.VILLAGE)

    private val notifier = mockk<WeatherNotifier>(relaxed = true)

    private class FakeRegionRepository(private val region: Region?) : RegionRepository {
        override fun observeSelectedRegion(): Flow<Region?> = flowOf(region)
        override suspend fun getRegions(
            level: RegionLevel,
            parentCode: String?,
            forceRefresh: Boolean,
        ): NetworkResult<List<Region>> = NetworkResult.Success(emptyList())

        override suspend fun saveSelectedRegion(region: Region) = Unit
    }

    private fun forecast(hourlyCode: Int) = Forecast(
        regionCode = keudeBakongan.code,
        regionName = keudeBakongan.name,
        days = listOf(
            DailyForecast(
                date = LocalDate.of(2026, 8, 31),
                temperatureMax = 31.0,
                temperatureMin = 24.0,
                precipitationMm = 0.0,
                windSpeed = 4.0,
                weatherCode = 1,
                description = null,
                source = WeatherSource.BMKG,
                hourly = listOf(
                    HourlyForecast(
                        time = LocalDateTime.of(LocalDate.of(2026, 8, 31), LocalTime.of(13, 0)),
                        temperatureCelsius = 28.0,
                        humidityPercent = 80.0,
                        precipitationMm = 0.0,
                        windSpeed = 4.0,
                        weatherCode = hourlyCode,
                        description = null,
                    ),
                ),
            ),
        ),
        source = WeatherSource.BMKG,
        fetchedAt = 0L,
    )

    private fun buildWorker(
        region: Region?,
        hasil: NetworkResult<Forecast>,
    ): WeatherCheckWorker {
        val weatherRepository = mockk<WeatherRepository>()
        io.mockk.coEvery { weatherRepository.getForecast(any(), any()) } returns hasil

        val factory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker = WeatherCheckWorker(
                appContext = appContext,
                params = workerParameters,
                getForecast = GetForecastUseCase(weatherRepository),
                observeSelectedRegion = ObserveSelectedRegionUseCase(
                    FakeRegionRepository(region),
                ),
                notifier = notifier,
            )
        }

        return TestListenableWorkerBuilder<WeatherCheckWorker>(context)
            .setWorkerFactory(factory)
            .build()
    }

    @Test
    fun tanpaWilayahTerpilihPekerjaanDianggapSelesaiBukanGagal() = runTest {
        // Belum memilih wilayah adalah keadaan sah. Mengembalikan retry akan
        // membuat WorkManager mencoba terus tanpa akhir tanpa hasil apa pun.
        val worker = buildWorker(region = null, hasil = NetworkResult.Success(forecast(1)))

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun kegagalanJaringanMemintaDicobaLagiBukanDinyatakanGagal() = runTest {
        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Error("Tidak ada koneksi"),
        )

        val result = worker.doWork()

        // failure() akan membuat pemeriksaan ini tidak pernah dicoba lagi —
        // padahal jaringan desa yang putus hampir selalu pulih sendiri.
        assertTrue(result is ListenableWorker.Result.Retry)
        verify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun cuacaAmanTidakMemunculkanNotifikasi() = runTest {
        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 1)),
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun cuacaEkstremMemunculkanTepatSatuNotifikasi() = runTest {
        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 95)),
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 1) { notifier.notify(match<WeatherAlert> { it.regionName == "Keude Bakongan" }) }
    }
}
