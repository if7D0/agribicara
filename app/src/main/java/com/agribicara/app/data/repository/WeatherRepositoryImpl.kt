package com.agribicara.app.data.repository

import android.content.Context
import com.agribicara.app.R
import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.DispatcherProvider
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.dao.WeatherCacheDao
import com.agribicara.app.data.local.entity.WeatherCacheEntity
import com.agribicara.app.data.mapper.BmkgMapper
import com.agribicara.app.data.mapper.ForecastMerger
import com.agribicara.app.data.mapper.OpenMeteoMapper
import com.agribicara.app.data.remote.bmkg.BmkgApiService
import com.agribicara.app.data.remote.openmeteo.OpenMeteoApiService
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.WeatherSource
import com.agribicara.app.domain.repository.WeatherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Rantai fallback cuaca.
 *
 * ```
 * cache masih segar? --ya--> pakai cache, TANPA jaringan sama sekali
 *   |
 *  tidak
 *   v
 * BMKG (hari 1-3) --sukses--> digabung Open-Meteo (hari 4-7) --> tampil
 *   |
 *   +--gagal/404---> Open-Meteo saja (hari 1-7, ditandai estimasi)
 *                       |
 *                       +--gagal--> cache basi (ditandai CACHE)
 *                                      |
 *                                      +--kosong--> Error
 * ```
 *
 * Tidak ada exception yang boleh lolos dari kelas ini KECUALI
 * [CancellationException] — itu justru wajib diteruskan, lihat [callOrNull].
 */
@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val bmkgApi: BmkgApiService,
    private val openMeteoApi: OpenMeteoApiService,
    private val weatherCacheDao: WeatherCacheDao,
    private val userPreferenceDao: UserPreferenceDao,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
    @ApplicationContext private val context: Context,
) : WeatherRepository {

    override suspend fun getForecast(
        regionCode: String,
        regionName: String,
    ): NetworkResult<Forecast> = withContext(dispatchers.io) {
        val today = LocalDate.now(clock.withZone(ZoneId.of(Constants.DEFAULT_TIMEZONE)))
        val cached = readCache(regionCode, today)

        // Cache yang masih segar dipakai apa adanya, tanpa menyentuh jaringan.
        // Tanpa penjaga ini setiap kali layar resume (buka kunci layar, kembali
        // dari app lain) memicu dua request HTTPS — beban kuota nyata bagi
        // petani yang memakai data seluler.
        if (cached.isNotEmpty() && isFresh(cached)) {
            return@withContext NetworkResult.Success(
                Forecast(
                    regionCode = regionCode,
                    regionName = regionName,
                    days = cached.map { it.forecast },
                    // Segar, jadi BUKAN mode offline: pakai sumber asli hari pertama.
                    source = cached.first().forecast.source,
                    fetchedAt = cached.maxOf { it.fetchedAt },
                ),
            )
        }

        val bmkgResult = fetchBmkg(regionCode)
        val bmkgDays = bmkgResult.days
        // BMKG mengirim desa + kecamatan ("Cibeureum, Cimahi Selatan"),
        // lebih informatif daripada nama desa saja dari region picker.
        val displayName = bmkgResult.regionName ?: regionName
        val coordinates = loadCoordinates()

        // Open-Meteo dipanggil bila BMKG kurang dari target hari, ATAU bila
        // BMKG gagal total. Keduanya butuh koordinat yang hanya bisa kita
        // ketahui dari BMKG sebelumnya.
        val needsOpenMeteo = bmkgDays.size < Constants.FORECAST_DAYS
        val openMeteoDays = if (needsOpenMeteo && coordinates != null) {
            fetchOpenMeteo(coordinates.first, coordinates.second)
        } else {
            emptyList()
        }

        val merged = ForecastMerger.merge(
            primary = bmkgDays,
            secondary = openMeteoDays,
            today = today,
        )

        if (merged.isNotEmpty()) {
            persist(regionCode, merged)
            return@withContext NetworkResult.Success(
                Forecast(
                    regionCode = regionCode,
                    regionName = displayName,
                    days = merged,
                    source = merged.first().source,
                    fetchedAt = clock.millis(),
                ),
            )
        }

        // Seluruh jaringan gagal — pakai cache walaupun sudah basi.
        Timber.w("BMKG dan Open-Meteo sama-sama gagal, memakai cache")
        if (cached.isEmpty()) {
            NetworkResult.Error(context.getString(R.string.error_no_cached_weather))
        } else {
            NetworkResult.Success(
                Forecast(
                    regionCode = regionCode,
                    regionName = displayName,
                    days = cached.map { it.forecast },
                    // Basi DAN jaringan mati: inilah mode offline sesungguhnya.
                    source = WeatherSource.CACHE,
                    // Waktu tulis cache yang sebenarnya, bukan waktu sekarang —
                    // UI memakainya untuk memberi tahu seberapa lama data ini.
                    fetchedAt = cached.maxOf { it.fetchedAt },
                ),
            )
        }
    }

    /** Hasil satu panggilan BMKG: prakiraan plus nama wilayah versi BMKG. */
    private data class BmkgResult(
        val days: List<DailyForecast>,
        val regionName: String?,
    )

    /** Satu hari dari cache beserta waktu penulisannya. */
    private data class CachedDay(
        val forecast: DailyForecast,
        val fetchedAt: Long,
    )

    private fun isFresh(cached: List<CachedDay>): Boolean {
        val newest = cached.maxOf { it.fetchedAt }
        val ageMillis = clock.millis() - newest
        // Waktu mundur (pengguna mengubah jam perangkat) menghasilkan umur
        // negatif; perlakukan sebagai basi agar tidak terkunci pada data lama.
        if (ageMillis < 0) return false
        return ageMillis < TimeUnit.HOURS.toMillis(Constants.CACHE_STALE_HOURS)
    }

    private suspend fun fetchBmkg(regionCode: String): BmkgResult = callOrNull("cuaca BMKG") {
        val response = bmkgApi.getForecast(regionCode)
        if (!response.isSuccessful) {
            // Retrofit TIDAK melempar untuk 4xx/5xx — 404 "Data not found"
            // sampai ke sini sebagai response yang secara teknis sukses.
            Timber.w("BMKG membalas HTTP %d", response.code())
            return@callOrNull null
        }
        val body = response.body() ?: return@callOrNull null

        // Koordinat kelurahan hanya tersedia di sini. Simpan supaya
        // Open-Meteo tetap bisa dipakai saat BMKG mati di kemudian hari.
        body.lokasi?.let { lokasi ->
            val lat = lokasi.lat
            val lon = lokasi.lon
            if (lat != null && lon != null) {
                callOrNull("simpan koordinat") {
                    userPreferenceDao.updateCoordinates(Constants.USER_PREFERENCE_ID, lat, lon)
                }
            }
        }

        BmkgResult(
            days = BmkgMapper.toDailyForecasts(body),
            regionName = BmkgMapper.regionDisplayName(body),
        )
    } ?: BmkgResult(emptyList(), null)

    private suspend fun fetchOpenMeteo(lat: Double, lon: Double): List<DailyForecast> =
        callOrNull("cuaca Open-Meteo") {
            val response = openMeteoApi.getForecast(latitude = lat, longitude = lon)
            if (!response.isSuccessful) {
                Timber.w("Open-Meteo membalas HTTP %d", response.code())
                return@callOrNull null
            }
            response.body()?.let(OpenMeteoMapper::toDailyForecasts)
        }.orEmpty()

    private suspend fun loadCoordinates(): Pair<Double, Double>? =
        callOrNull("baca koordinat tersimpan") {
            val preference = userPreferenceDao.get(Constants.USER_PREFERENCE_ID)
            val lat = preference?.latitude
            val lon = preference?.longitude
            if (lat != null && lon != null) lat to lon else null
        }

    private suspend fun persist(regionCode: String, days: List<DailyForecast>) {
        callOrNull("simpan cache cuaca") {
            weatherCacheDao.replaceForRegion(regionCode, days.map { it.toEntity(regionCode) })
        }
    }

    private suspend fun readCache(regionCode: String, today: LocalDate): List<CachedDay> =
        callOrNull("baca cache cuaca") {
            weatherCacheDao.getForRegion(regionCode)
                .mapNotNull { entity -> entity.toDomain()?.let { CachedDay(it, entity.fetchedAt) } }
                .filter { !it.forecast.date.isBefore(today) }
                .sortedBy { it.forecast.date }
        }.orEmpty()

    /**
     * Menjalankan [block] dan mengubah kegagalan menjadi null.
     *
     * [CancellationException] SENGAJA dilempar ulang. `runCatching` menangkap
     * Throwable termasuk pembatalan coroutine, sehingga layar yang sudah
     * ditinggalkan pengguna akan tetap melanjutkan panggilan jaringan
     * berikutnya dan tetap menulis ke database — structured concurrency rusak
     * tanpa gejala yang terlihat.
     */
    private suspend inline fun <T : Any> callOrNull(
        what: String,
        block: () -> T?,
    ): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Gagal: %s", what)
        null
    }

    private fun DailyForecast.toEntity(regionCode: String) = WeatherCacheEntity(
        regionCode = regionCode,
        date = date.toString(),
        temperatureMax = temperatureMax,
        temperatureMin = temperatureMin,
        precipitationMm = precipitationMm,
        windSpeed = windSpeed,
        weatherCode = weatherCode,
        description = description,
        source = source.name,
        fetchedAt = clock.millis(),
    )

    /** Baris cache yang tanggalnya rusak dibuang, bukan meruntuhkan seluruh baca. */
    private fun WeatherCacheEntity.toDomain(): DailyForecast? = try {
        DailyForecast(
            date = LocalDate.parse(date),
            temperatureMax = temperatureMax,
            temperatureMin = temperatureMin,
            precipitationMm = precipitationMm,
            windSpeed = windSpeed,
            weatherCode = weatherCode,
            description = description,
            // Sumber ASLI dipertahankan, bukan ditimpa CACHE: status offline
            // sudah disampaikan lewat Forecast.source di tingkat atas, dan
            // menimpanya di sini akan menghapus badge "Data estimasi" sehingga
            // hari 4-7 tampak sepasti data BMKG padahal bukan.
            source = parseSource(source),
        )
    } catch (e: DateTimeParseException) {
        Timber.e(e, "Baris cache dengan tanggal tidak valid: %s", date)
        null
    }

    /** Nilai enum yang tidak dikenali (mis. dari versi app lama) jatuh ke CACHE. */
    private fun parseSource(raw: String): WeatherSource =
        WeatherSource.entries.firstOrNull { it.name == raw } ?: WeatherSource.CACHE
}
