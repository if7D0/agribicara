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
import com.agribicara.app.data.notification.WeatherAlertNotifier
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

    private val notifier = FakeNotifier()

    private val sentAlertDao = FakeSentAlertDao()

    private val clock: Clock = Clock.fixed(SEKARANG.atZone(ZONA).toInstant(), ZONA)

    private class FakeRegionRepository(private val region: Region?) : RegionRepository {
        override fun observeSelectedRegion(): Flow<Region?> = flowOf(region)
        override suspend fun getRegions(
            level: RegionLevel,
            parentCode: String?,
            forceRefresh: Boolean,
        ): NetworkResult<List<Region>> = NetworkResult.Success(emptyList())

        override suspend fun saveSelectedRegion(region: Region) = Unit
    }

    /**
     * Pencatat notifikasi, pengganti mock.
     *
     * Ditulis tangan dan BUKAN MockK. Agen Android MockK menyuntikkan JAR ke
     * boot classpath lalu meng-instrumentasi java.lang.Object, dan di Android
     * 15 dengan targetSdk 37 empat akses hidden-API yang dibutuhkannya ditolak.
     * Biayanya membengkak seiring banyaknya kelas termuat di proses, sehingga
     * kelas ini — dulu satu-satunya pemakai MockK di seluruh androidTest —
     * menahan suite penuh lebih dari 150 detik begitu tujuh kelas DAO berjalan
     * lebih dulu, sementara ia lulus dalam 1,3 detik bila dijalankan sendiri.
     * Lihat [WeatherAlertNotifier].
     */
    private class FakeNotifier : WeatherAlertNotifier {

        /** Setiap panggilan notify, berhasil maupun tidak. */
        val dicoba = mutableListOf<WeatherAlert>()

        /** Hanya yang benar-benar diserahkan ke sistem. */
        val terkirim = mutableListOf<WeatherAlert>()

        /** Nilai balik notify. false meniru izin notifikasi yang belum ada. */
        var berhasilTampil: Boolean = true

        override fun notify(alert: WeatherAlert): Boolean {
            dicoba += alert
            if (berhasilTampil) terkirim += alert
            return berhasilTampil
        }
    }

    private class FakeSentAlertDao : SentAlertDao {

        /** Baris yang sudah ada sebelum worker berjalan; null berarti kosong. */
        var tersimpan: SentAlertEntity? = null

        /** Setiap upsert yang benar-benar terjadi. */
        val dicatat = mutableListOf<SentAlertEntity>()

        override suspend fun get(id: Int): SentAlertEntity? = tersimpan

        override suspend fun upsert(entity: SentAlertEntity) {
            dicatat += entity
            tersimpan = entity
        }
    }

    private class FakeWeatherRepository(
        private val hasil: NetworkResult<Forecast>,
    ) : WeatherRepository {
        override suspend fun getForecast(
            regionCode: String,
            regionName: String,
        ): NetworkResult<Forecast> = hasil
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
        val weatherRepository = FakeWeatherRepository(hasil)

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
        assertEquals(0, notifier.dicoba.size)
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
        assertEquals(0, notifier.dicoba.size)
    }

    @Test
    fun cuacaAmanTidakMemunculkanNotifikasi() = runTest {
        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 1)),
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(0, notifier.dicoba.size)
    }

    @Test
    fun cuacaEkstremMemunculkanTepatSatuNotifikasi() = runTest {
        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 95)),
        )

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(1, notifier.dicoba.size)
        assertEquals("Keude Bakongan", notifier.terkirim.single().regionName)
    }

    // --- H3: peringatan yang sama tidak diulang ----------------------------

    @Test
    fun peringatanYangSudahPernahDikirimTidakDiulang() = runTest {
        // Inti H3. Pemeriksaan berjalan tiap 6 jam sementara jangkauannya 2
        // hari, jadi satu badai yang sama akan ditemukan berkali-kali. Tanpa
        // penjaga ini petani menerima notifikasi yang sama empat kali sehari,
        // lalu mematikan notifikasi untuk selamanya.
        sentAlertDao.tersimpan = SentAlertEntity(
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
        assertEquals(0, notifier.dicoba.size)
    }

    @Test
    fun peringatanDenganAlasanBerbedaTetapDikirim() = runTest {
        // Kejadian yang berbeda layak mengganggu lagi. Badai yang berubah
        // menjadi hujan lebat bukan pengulangan.
        sentAlertDao.tersimpan = SentAlertEntity(
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

        assertEquals(1, notifier.dicoba.size)
    }

    @Test
    fun peringatanDicatatHanyaSetelahBenarBenarTampil() = runTest {
        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 95)),
        )

        worker.doWork()

        val dicatat = sentAlertDao.dicatat.single()
        assertEquals(keudeBakongan.code, dicatat.regionCode)
        assertEquals("THUNDERSTORM", dicatat.reason)
    }

    @Test
    fun peringatanYangGagalTampilTidakDicatatSebagaiTerkirim() = runTest {
        // Kalau izin notifikasi belum diberikan, notify() mengembalikan false.
        // Mencatatnya sebagai terkirim akan membungkam kejadian itu selamanya,
        // bahkan setelah petani akhirnya memberikan izinnya.
        notifier.berhasilTampil = false

        val worker = buildWorker(
            region = keudeBakongan,
            hasil = NetworkResult.Success(forecast(hourlyCode = 95)),
        )

        worker.doWork()

        assertEquals(0, sentAlertDao.dicatat.size)
    }
}
