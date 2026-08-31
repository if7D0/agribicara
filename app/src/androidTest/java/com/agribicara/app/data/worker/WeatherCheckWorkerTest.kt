package com.agribicara.app.data.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.dao.SentAlertDao
import com.agribicara.app.data.local.entity.SentAlertEntity
import com.agribicara.app.data.notification.AlertHistory
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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
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
 * Keputusan di dalam `doWork` diuji, dan semuanya punya konsekuensi nyata:
 * belum ada wilayah bukan kegagalan, kegagalan jaringan harus retry bukan
 * failure, cuaca aman tidak boleh mengganggu siapa pun, dan sejak perbaikan H3
 * peringatan yang sama tidak boleh dikirim dua kali.
 *
 * Jam sengaja DIBEKUKAN. Setelah perbaikan H2, aturan cuaca membandingkan slot
 * jam terhadap waktu sekarang, jadi memakai jam sistem akan membuat test ini
 * lulus hari ini dan gagal besok tanpa ada kode yang berubah.
 */
@RunWith(AndroidJUnit4::class)
class WeatherCheckWorkerTest {

    private companion object {
        val ZONA: ZoneId = ZoneId.of("Asia/Jakarta")
        val HARI_INI: LocalDate = LocalDate.of(2026, 8, 31)

        /** Pagi: slot pukul 13.00 pada fixture masih di depan. */
        val SEKARANG: LocalDateTime = LocalDateTime.of(HARI_INI, LocalTime.of(6, 0))
    }

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val keudeBakongan = Region("11.01.01.2001", "Keude Bakongan", RegionLevel.VILLAGE)

    private val notifier = mockk<WeatherNotifier>(relaxed = true)

    private val sentAlertDao = mockk<SentAlertDao>(relaxed = true)

    private val clock: Clock = Clock.fixed(SEKARANG.atZone(ZONA).toInstant(), ZONA)

    @Before
    fun siapkanBawaan() {
        // relaxed = true mengembalikan false untuk Boolean, dan false berarti
        // "notifikasi tidak jadi tampil". Bawaannya harus true supaya jalur
        // normal yang diuji adalah notifikasi yang BERHASIL tampil.
        every { notifier.notify(any()) } returns true
        coEvery { sentAlertDao.get(any()) } returns null
    }

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
                date = HARI_INI,
                temperatureMax = 31.0,
                temperatureMin = 24.0,
                precipitationMm = 0.0,
                windSpeed = 4.0,
                weatherCode = 1,
                description = null,
                source = WeatherSource.BMKG,
                hourly = listOf(
                    HourlyForecast(
                        time = LocalDateTime.of(HARI_INI, LocalTime.of(13, 0)),
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
        coEvery { weatherRepository.getForecast(any(), any()) } returns hasil

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
                alertHistory = AlertHistory(sentAlertDao, clock),
                clock = clock,
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
        verify(exactly = 1) {
            notifier.notify(match<WeatherAlert> { it.regionName == "Keude Bakongan" })
        }
    }

    // --- H3: peringatan yang sama tidak diulang ----------------------------

    @Test
    fun peringatanYangSudahPernahDikirimTidakDiulang() = runTest {
        // Inti H3. Pemeriksaan berjalan tiap 6 jam sementara jangkauannya 2
        // hari, jadi satu badai yang sama akan ditemukan berkali-kali. Tanpa
        // penjaga ini petani menerima notifikasi yang sama empat kali sehari,
        // lalu mematikan notifikasi untuk selamanya.
        coEvery { sentAlertDao.get(any()) } returns SentAlertEntity(
            regionCode = keudeBakongan.code,
            date = HARI_INI.toString(),
            reason = "THUNDERSTORM",
            notifiedAt = 0L,
        )

        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 95)),
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        verify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun peringatanDenganAlasanBerbedaTetapDikirim() = runTest {
        // Kejadian yang berbeda layak mengganggu lagi. Badai yang berubah
        // menjadi hujan lebat bukan pengulangan.
        coEvery { sentAlertDao.get(any()) } returns SentAlertEntity(
            regionCode = keudeBakongan.code,
            date = HARI_INI.toString(),
            reason = "HEAVY_RAIN",
            notifiedAt = 0L,
        )

        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 95)),
        )

        worker.doWork()

        verify(exactly = 1) { notifier.notify(any()) }
    }

    @Test
    fun peringatanDicatatHanyaSetelahBenarBenarTampil() = runTest {
        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 95)),
        )

        worker.doWork()

        coVerify(exactly = 1) {
            sentAlertDao.upsert(
                match<SentAlertEntity> {
                    it.regionCode == keudeBakongan.code && it.reason == "THUNDERSTORM"
                },
            )
        }
    }

    @Test
    fun peringatanYangGagalTampilTidakDicatatSebagaiTerkirim() = runTest {
        // Kalau izin notifikasi belum diberikan, notify() mengembalikan false.
        // Mencatatnya sebagai terkirim akan membungkam kejadian itu selamanya,
        // bahkan setelah petani akhirnya memberikan izinnya.
        every { notifier.notify(any()) } returns false

        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 95)),
        )

        worker.doWork()

        coVerify(exactly = 0) { sentAlertDao.upsert(any()) }
    }
}
