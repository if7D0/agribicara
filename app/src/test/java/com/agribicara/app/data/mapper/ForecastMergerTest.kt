package com.agribicara.app.data.mapper

import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.HourlyForecast
import com.agribicara.app.domain.model.WeatherSource
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Penggabungan BMKG (hari 1-3) + Open-Meteo (hari 4-7).
 *
 * Inilah yang membuat success signal PRD "cuaca 7 hari" tetap terpenuhi
 * walaupun BMKG hanya menyediakan tiga hari.
 */
class ForecastMergerTest {

    private val today = LocalDate.of(2026, 8, 29)

    private fun day(date: LocalDate, source: WeatherSource, temp: Double = 30.0) = DailyForecast(
        date = date,
        temperatureMax = temp,
        temperatureMin = temp - 10,
        precipitationMm = 0.0,
        windSpeed = 5.0,
        weatherCode = 1,
        description = if (source == WeatherSource.BMKG) "Cerah" else null,
        source = source,
    )

    @Test
    fun `BMKG dan Open-Meteo digabung menjadi tujuh hari`() {
        val bmkg = (0..2).map { day(today.plusDays(it.toLong()), WeatherSource.BMKG) }
        val openMeteo = (0..6).map { day(today.plusDays(it.toLong()), WeatherSource.OPEN_METEO) }

        val merged = ForecastMerger.merge(bmkg, openMeteo, today)

        assertEquals(7, merged.size)
    }

    @Test
    fun `BMKG menang pada tanggal yang tumpang tindih`() {
        val bmkg = (0..2).map { day(today.plusDays(it.toLong()), WeatherSource.BMKG, temp = 28.0) }
        val openMeteo = (0..6).map {
            day(today.plusDays(it.toLong()), WeatherSource.OPEN_METEO, temp = 99.0)
        }

        val merged = ForecastMerger.merge(bmkg, openMeteo, today)

        assertEquals(
            listOf(
                WeatherSource.BMKG, WeatherSource.BMKG, WeatherSource.BMKG,
                WeatherSource.OPEN_METEO, WeatherSource.OPEN_METEO,
                WeatherSource.OPEN_METEO, WeatherSource.OPEN_METEO,
            ),
            merged.map { it.source },
        )
        assertEquals(28.0, merged[0].temperatureMax!!, 0.001)
    }

    @Test
    fun `hasil selalu terurut menaik`() {
        val bmkg = listOf(day(today.plusDays(2), WeatherSource.BMKG))
        val openMeteo = listOf(
            day(today.plusDays(5), WeatherSource.OPEN_METEO),
            day(today, WeatherSource.OPEN_METEO),
        )

        val merged = ForecastMerger.merge(bmkg, openMeteo, today)

        assertEquals(merged.map { it.date }.sorted(), merged.map { it.date })
    }

    @Test
    fun `tanggal yang sudah lewat dibuang`() {
        // Bisa muncul dari cache lama; menampilkannya menyesatkan petani.
        val stale = listOf(
            day(today.minusDays(3), WeatherSource.CACHE),
            day(today, WeatherSource.CACHE),
        )

        val merged = ForecastMerger.merge(stale, emptyList(), today)

        assertEquals(1, merged.size)
        assertEquals(today, merged[0].date)
    }

    @Test
    fun `hasil dipotong pada batas hari`() {
        val openMeteo = (0..15).map { day(today.plusDays(it.toLong()), WeatherSource.OPEN_METEO) }

        val merged = ForecastMerger.merge(emptyList(), openMeteo, today, limit = 7)

        assertEquals(7, merged.size)
    }

    @Test
    fun `BMKG saja tetap sah bila Open-Meteo gagal`() {
        val bmkg = (0..2).map { day(today.plusDays(it.toLong()), WeatherSource.BMKG) }

        val merged = ForecastMerger.merge(bmkg, emptyList(), today)

        assertEquals(3, merged.size)
        assertTrue(merged.all { it.source == WeatherSource.BMKG })
    }

    @Test
    fun `Open-Meteo saja tetap sah bila BMKG gagal`() {
        val openMeteo = (0..6).map { day(today.plusDays(it.toLong()), WeatherSource.OPEN_METEO) }

        val merged = ForecastMerger.merge(emptyList(), openMeteo, today)

        assertEquals(7, merged.size)
        assertTrue(merged.all { it.source == WeatherSource.OPEN_METEO })
    }

    @Test
    fun `dua sumber kosong menghasilkan daftar kosong`() {
        assertEquals(emptyList<Any>(), ForecastMerger.merge(emptyList(), emptyList(), today))
    }

    /** Hari BMKG yang rincian jamnya hanya sore — persis grup pertama BMKG. */
    private fun partialDay(date: LocalDate, temp: Double) = day(date, WeatherSource.BMKG, temp)
        .copy(
            hourly = listOf(17, 20, 23).map { hour ->
                HourlyForecast(
                    time = date.atTime(hour, 0),
                    temperatureCelsius = temp,
                    humidityPercent = null,
                    precipitationMm = 0.0,
                    windSpeed = null,
                    weatherCode = 1,
                    description = "Cerah",
                )
            },
        )

    /** Hari BMKG lengkap: rincian jam mulai dini hari. */
    private fun fullDay(date: LocalDate, temp: Double) = day(date, WeatherSource.BMKG, temp)
        .copy(
            hourly = listOf(2, 8, 14, 20).map { hour ->
                HourlyForecast(
                    time = date.atTime(hour, 0),
                    temperatureCelsius = temp,
                    humidityPercent = null,
                    precipitationMm = 0.0,
                    windSpeed = null,
                    weatherCode = 1,
                    description = "Cerah",
                )
            },
        )

    @Test
    fun `hari BMKG parsial dilebarkan oleh Open-Meteo`() {
        // Grup hari pertama BMKG mulai sore, jadi maksimumnya hanya mewakili
        // sisa hari. Membuka app pukul 17:00 tidak boleh menampilkan 25 derajat
        // padahal puncak hari itu 31.
        val bmkgSore = partialDay(today, 25.0)
        val openMeteoPenuh = day(today, WeatherSource.OPEN_METEO, temp = 31.0)

        val merged = ForecastMerger.merge(listOf(bmkgSore), listOf(openMeteoPenuh), today)

        assertEquals(31.0, merged[0].temperatureMax!!, 0.001)
        assertEquals(15.0, merged[0].temperatureMin!!, 0.001)
    }

    @Test
    fun `hari BMKG LENGKAP tidak dicemari estimasi grid Open-Meteo`() {
        // Di hari lengkap, pengukuran level kelurahan lebih dipercaya daripada
        // estimasi grid — menggabung di sini menghapus keunggulan BMKG.
        val bmkgLengkap = fullDay(today, 28.0)
        val openMeteoNgawur = day(today, WeatherSource.OPEN_METEO, temp = 99.0)

        val merged = ForecastMerger.merge(listOf(bmkgLengkap), listOf(openMeteoNgawur), today)

        assertEquals(28.0, merged[0].temperatureMax!!, 0.001)
    }

    @Test
    fun `hari tanpa rincian jam tidak dianggap parsial`() {
        // Baris dari cache tidak membawa rincian per jam; menebak lebih
        // berbahaya daripada membiarkan.
        val dariCache = day(today, WeatherSource.BMKG, temp = 25.0)
        val openMeteo = day(today, WeatherSource.OPEN_METEO, temp = 31.0)

        val merged = ForecastMerger.merge(listOf(dariCache), listOf(openMeteo), today)

        assertEquals(25.0, merged[0].temperatureMax!!, 0.001)
    }

    @Test
    fun `penggabungan suhu tidak mengubah deskripsi dan sumber BMKG`() {
        val merged = ForecastMerger.merge(
            listOf(partialDay(today, 25.0)),
            listOf(day(today, WeatherSource.OPEN_METEO, temp = 31.0)),
            today,
        )

        assertEquals(WeatherSource.BMKG, merged[0].source)
        assertEquals("Cerah", merged[0].description)
    }

    @Test
    fun `suhu BMKG dipertahankan bila sudah lebih ekstrem`() {
        val merged = ForecastMerger.merge(
            listOf(partialDay(today, 33.0)),
            listOf(day(today, WeatherSource.OPEN_METEO, temp = 30.0)),
            today,
        )

        assertEquals(33.0, merged[0].temperatureMax!!, 0.001)
    }

    @Test
    fun `hari parsial tanpa padanan Open-Meteo tidak berubah`() {
        val merged = ForecastMerger.merge(
            listOf(partialDay(today, 25.0)),
            listOf(day(today.plusDays(3), WeatherSource.OPEN_METEO, temp = 31.0)),
            today,
        )

        assertEquals(25.0, merged[0].temperatureMax!!, 0.001)
    }
}
