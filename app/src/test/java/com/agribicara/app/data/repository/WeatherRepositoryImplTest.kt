package com.agribicara.app.data.repository

import android.content.Context
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.dao.WeatherCacheDao
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import com.agribicara.app.data.local.entity.WeatherCacheEntity
import com.agribicara.app.data.remote.bmkg.BmkgApiService
import com.agribicara.app.data.remote.bmkg.dto.BmkgForecastResponse
import com.agribicara.app.data.remote.openmeteo.OpenMeteoApiService
import com.agribicara.app.data.remote.openmeteo.dto.OpenMeteoResponse
import com.agribicara.app.domain.model.WeatherSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Rantai fallback: BMKG -> Open-Meteo -> cache -> Error.
 *
 * Ini logika paling berisiko di Fase 2 — kegagalan di sini berarti petani
 * melihat layar kosong justru ketika koneksi sedang buruk, yaitu saat data
 * cuaca paling dibutuhkan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WeatherRepositoryImplTest {

    private val bmkgApi = mockk<BmkgApiService>()
    private val openMeteoApi = mockk<OpenMeteoApiService>()
    private val weatherCacheDao = mockk<WeatherCacheDao>(relaxed = true)
    private val userPreferenceDao = mockk<UserPreferenceDao>(relaxed = true)
    private val context = mockk<Context>()

    // Jam tetap: 2026-08-29 10:00 UTC = 17:00 WIB, sama dengan fixture BMKG.
    private val clock = Clock.fixed(Instant.parse("2026-08-29T10:00:00Z"), ZoneId.of("UTC"))

    private val dispatchers = object : DispatcherProvider {
        private val test = UnconfinedTestDispatcher()
        override val io = test
        override val default = test
        override val main = test
    }

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private lateinit var repository: WeatherRepositoryImpl

    @Before
    fun setUp() {
        every { context.getString(any()) } returns "pesan error"
        repository = WeatherRepositoryImpl(
            bmkgApi = bmkgApi,
            openMeteoApi = openMeteoApi,
            weatherCacheDao = weatherCacheDao,
            userPreferenceDao = userPreferenceDao,
            dispatchers = dispatchers,
            clock = clock,
            context = context,
        )
    }

    private fun bmkgSample(): BmkgForecastResponse {
        val raw = requireNotNull(javaClass.classLoader?.getResourceAsStream("bmkg_sample.json"))
            .bufferedReader().use { it.readText() }
        return json.decodeFromString(BmkgForecastResponse.serializer(), raw)
    }

    private fun openMeteoSample(): OpenMeteoResponse {
        val raw = requireNotNull(javaClass.classLoader?.getResourceAsStream("openmeteo_sample.json"))
            .bufferedReader().use { it.readText() }
        return json.decodeFromString(OpenMeteoResponse.serializer(), raw)
    }

    private fun <T> notFound(): Response<T> = Response.error(
        404,
        """{"message":"Data not found","error":"Not Found","statusCode":404}"""
            .toResponseBody("application/json".toMediaType()),
    )

    private fun preferenceWithCoordinates() = UserPreferenceEntity(
        id = Constants.USER_PREFERENCE_ID,
        isOnboardingCompleted = true,
        regionCode = "32.77.01.1002",
        regionName = "Cibeureum",
        latitude = -6.9079,
        longitude = 107.5590,
    )

    @Test
    fun `BMKG sukses digabung Open-Meteo menjadi tujuh hari`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } returns Response.success(bmkgSample())
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } returns
            Response.success(openMeteoSample())

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        assertTrue(result is NetworkResult.Success)
        val forecast = (result as NetworkResult.Success).data
        assertEquals(7, forecast.days.size)
        // Tiga hari pertama harus dari BMKG, sisanya estimasi.
        assertEquals(WeatherSource.BMKG, forecast.days[0].source)
        assertEquals(WeatherSource.BMKG, forecast.days[2].source)
        assertEquals(WeatherSource.OPEN_METEO, forecast.days[3].source)
    }

    @Test
    fun `BMKG 404 jatuh ke Open-Meteo`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } returns notFound()
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } returns
            Response.success(openMeteoSample())

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        assertTrue(result is NetworkResult.Success)
        val forecast = (result as NetworkResult.Success).data
        assertEquals(7, forecast.days.size)
        assertTrue(forecast.days.all { it.source == WeatherSource.OPEN_METEO })
    }

    @Test
    fun `kegagalan jaringan BMKG tidak melempar exception`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } throws IOException("jaringan mati")
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } returns
            Response.success(openMeteoSample())

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        assertTrue(result is NetworkResult.Success)
    }

    @Test
    fun `kedua API gagal maka data dibaca dari cache`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } throws IOException("mati")
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } throws
            IOException("mati")
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { weatherCacheDao.getForRegion(any()) } returns listOf(
            WeatherCacheEntity(
                regionCode = "32.77.01.1002",
                date = "2026-08-29",
                temperatureMax = 30.0,
                temperatureMin = 20.0,
                precipitationMm = 0.0,
                windSpeed = 5.0,
                weatherCode = 1,
                description = "Cerah",
                source = WeatherSource.BMKG.name,
                fetchedAt = 1_700_000_000_000,
            ),
        )

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        assertTrue(result is NetworkResult.Success)
        val forecast = (result as NetworkResult.Success).data
        assertEquals(WeatherSource.CACHE, forecast.source)
        // fetchedAt harus waktu tulis cache yang sebenarnya, BUKAN waktu sekarang.
        assertEquals(1_700_000_000_000, forecast.fetchedAt)
        // Sumber asli per hari dipertahankan supaya badge "Data estimasi"
        // tidak hilang saat offline.
        assertEquals(WeatherSource.BMKG, forecast.days[0].source)
    }

    @Test
    fun `sumber Open-Meteo dipertahankan lewat perjalanan cache`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } throws IOException("mati")
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } throws
            IOException("mati")
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { weatherCacheDao.getForRegion(any()) } returns listOf(
            WeatherCacheEntity(
                regionCode = "32.77.01.1002",
                date = "2026-08-29",
                temperatureMax = 31.0,
                temperatureMin = 18.0,
                precipitationMm = 0.4,
                windSpeed = 12.4,
                weatherCode = 51,
                description = null,
                source = WeatherSource.OPEN_METEO.name,
                fetchedAt = 1_700_000_000_000,
            ),
        )

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        val forecast = (result as NetworkResult.Success).data
        assertEquals(WeatherSource.OPEN_METEO, forecast.days[0].source)
        assertEquals(WeatherSource.CACHE, forecast.source)
    }

    @Test
    fun `nilai sumber tak dikenal di cache jatuh ke CACHE tanpa crash`() = runTest {
        // Bisa terjadi bila versi app lama menulis nama enum yang sudah dihapus.
        coEvery { bmkgApi.getForecast(any()) } throws IOException("mati")
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } throws
            IOException("mati")
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { weatherCacheDao.getForRegion(any()) } returns listOf(
            WeatherCacheEntity(
                regionCode = "32.77.01.1002",
                date = "2026-08-29",
                temperatureMax = 30.0,
                temperatureMin = 20.0,
                precipitationMm = 0.0,
                windSpeed = 5.0,
                weatherCode = 1,
                description = "Cerah",
                source = "SUMBER_YANG_SUDAH_TIDAK_ADA",
                fetchedAt = 1_700_000_000_000,
            ),
        )

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        val forecast = (result as NetworkResult.Success).data
        assertEquals(WeatherSource.CACHE, forecast.days[0].source)
    }

    @Test
    fun `semua gagal dan cache kosong menghasilkan Error`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } throws IOException("mati")
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } throws
            IOException("mati")
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { weatherCacheDao.getForRegion(any()) } returns emptyList()

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `cache berisi hanya tanggal lampau diperlakukan sebagai kosong`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } throws IOException("mati")
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } throws
            IOException("mati")
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { weatherCacheDao.getForRegion(any()) } returns listOf(
            WeatherCacheEntity(
                regionCode = "32.77.01.1002",
                date = "2026-08-01",
                temperatureMax = 30.0,
                temperatureMin = 20.0,
                precipitationMm = 0.0,
                windSpeed = 5.0,
                weatherCode = 1,
                description = "Cerah",
                source = WeatherSource.BMKG.name,
                fetchedAt = 1_700_000_000_000,
            ),
        )

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `tanpa koordinat tersimpan Open-Meteo tidak dipanggil`() = runTest {
        // Kasus nyata: BMKG gagal pada pengambilan PERTAMA, sehingga koordinat
        // kelurahan belum pernah diketahui. Open-Meteo butuh lat-lon, jadi
        // fallback memang belum bisa jalan — harus jatuh ke cache, bukan crash.
        coEvery { bmkgApi.getForecast(any()) } returns notFound()
        coEvery { userPreferenceDao.get(any()) } returns UserPreferenceEntity(
            isOnboardingCompleted = true,
            regionCode = "32.77.01.1002",
            regionName = "Cibeureum",
        )
        coEvery { weatherCacheDao.getForRegion(any()) } returns emptyList()

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { openMeteoApi.getForecast(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `koordinat dari response BMKG disimpan untuk fallback berikutnya`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } returns Response.success(bmkgSample())
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } returns
            Response.success(openMeteoSample())

        repository.getForecast("32.77.01.1002", "Cibeureum")

        coVerify { userPreferenceDao.updateCoordinates(any(), any(), any()) }
    }

    @Test
    fun `hasil sukses ditulis ke cache`() = runTest {
        coEvery { bmkgApi.getForecast(any()) } returns Response.success(bmkgSample())
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } returns
            Response.success(openMeteoSample())

        repository.getForecast("32.77.01.1002", "Cibeureum")

        coVerify { weatherCacheDao.replaceForRegion(eq("32.77.01.1002"), any()) }
    }

    private fun cacheRow(date: String, fetchedAt: Long, source: WeatherSource) =
        WeatherCacheEntity(
            regionCode = "32.77.01.1002",
            date = date,
            temperatureMax = 30.0,
            temperatureMin = 20.0,
            precipitationMm = 0.0,
            windSpeed = 5.0,
            weatherCode = 1,
            description = "Cerah",
            source = source.name,
            fetchedAt = fetchedAt,
        )

    @Test
    fun `cache masih segar dipakai tanpa menyentuh jaringan sama sekali`() = runTest {
        // Tanpa penjaga ini, tiap resume layar memicu dua request HTTPS —
        // beban kuota nyata bagi petani yang memakai data seluler.
        val satuJamLalu = clock.millis() - TimeUnit.HOURS.toMillis(1)
        coEvery { weatherCacheDao.getForRegion(any()) } returns
            listOf(cacheRow("2026-08-29", satuJamLalu, WeatherSource.BMKG))

        val result = repository.getForecast("32.77.01.1002", "Cibeureum")

        assertTrue(result is NetworkResult.Success)
        // Segar berarti BUKAN mode offline.
        assertEquals(WeatherSource.BMKG, (result as NetworkResult.Success).data.source)
        coVerify(exactly = 0) { bmkgApi.getForecast(any()) }
        coVerify(exactly = 0) { openMeteoApi.getForecast(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `cache yang sudah basi memicu pengambilan ulang`() = runTest {
        val tujuhJamLalu = clock.millis() - TimeUnit.HOURS.toMillis(7)
        coEvery { weatherCacheDao.getForRegion(any()) } returns
            listOf(cacheRow("2026-08-29", tujuhJamLalu, WeatherSource.BMKG))
        coEvery { bmkgApi.getForecast(any()) } returns Response.success(bmkgSample())
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } returns
            Response.success(openMeteoSample())

        repository.getForecast("32.77.01.1002", "Cibeureum")

        coVerify { bmkgApi.getForecast(any()) }
    }

    @Test
    fun `jam perangkat yang mundur tidak mengunci app pada cache lama`() = runTest {
        // fetchedAt di masa depan menghasilkan umur negatif; harus dianggap basi.
        val masaDepan = clock.millis() + TimeUnit.HOURS.toMillis(5)
        coEvery { weatherCacheDao.getForRegion(any()) } returns
            listOf(cacheRow("2026-08-29", masaDepan, WeatherSource.BMKG))
        coEvery { bmkgApi.getForecast(any()) } returns Response.success(bmkgSample())
        coEvery { userPreferenceDao.get(any()) } returns preferenceWithCoordinates()
        coEvery { openMeteoApi.getForecast(any(), any(), any(), any(), any()) } returns
            Response.success(openMeteoSample())

        repository.getForecast("32.77.01.1002", "Cibeureum")

        coVerify { bmkgApi.getForecast(any()) }
    }

    @Test
    fun `pembatalan coroutine diteruskan bukan ditelan sebagai kegagalan`() = runTest {
        // runCatching akan menangkap CancellationException dan membuat
        // repository melanjutkan panggilan berikutnya di coroutine yang sudah
        // dibatalkan — structured concurrency rusak tanpa gejala terlihat.
        coEvery { weatherCacheDao.getForRegion(any()) } returns emptyList()
        coEvery { bmkgApi.getForecast(any()) } throws CancellationException("dibatalkan")

        var terlempar = false
        try {
            repository.getForecast("32.77.01.1002", "Cibeureum")
        } catch (e: CancellationException) {
            terlempar = true
        }

        assertTrue("CancellationException harus diteruskan", terlempar)
        coVerify(exactly = 0) { openMeteoApi.getForecast(any(), any(), any(), any(), any()) }
    }
}
