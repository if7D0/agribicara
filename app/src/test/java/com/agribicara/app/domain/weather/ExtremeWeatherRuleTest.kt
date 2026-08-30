package com.agribicara.app.domain.weather

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.AlertReason
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.HourlyForecast
import com.agribicara.app.domain.model.WeatherSource
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kebijakan "seberapa buruk baru disebut ekstrem".
 *
 * Dua arah kesalahan sama-sama merugikan dan keduanya diuji: terlalu sensitif
 * membuat petani mematikan notifikasi (setelah itu peringatan yang benar pun
 * tidak sampai), terlalu tumpul membuat fitur ini tidak ada gunanya.
 */
class ExtremeWeatherRuleTest {

    private val hariIni = LocalDate.of(2026, 8, 31)

    private fun jam(hour: Int, code: Int?) = HourlyForecast(
        time = LocalDateTime.of(hariIni, java.time.LocalTime.of(hour, 0)),
        temperatureCelsius = 28.0,
        humidityPercent = 80.0,
        precipitationMm = 0.0,
        windSpeed = 4.0,
        weatherCode = code,
        description = null,
    )

    private fun hari(
        date: LocalDate = hariIni,
        code: Int? = 1,
        rain: Double? = 0.0,
        hourly: List<HourlyForecast> = emptyList(),
    ) = DailyForecast(
        date = date,
        temperatureMax = 31.0,
        temperatureMin = 24.0,
        precipitationMm = rain,
        windSpeed = 4.0,
        weatherCode = code,
        description = null,
        source = WeatherSource.BMKG,
        hourly = hourly,
    )

    private fun prakiraan(vararg days: DailyForecast) = Forecast(
        regionCode = "11.01.01.2001",
        regionName = "Keude Bakongan",
        days = days.toList(),
        source = WeatherSource.BMKG,
        fetchedAt = 0L,
    )

    @Test
    fun `badai pada satu slot jam terdeteksi meski hari itu didominasi cerah`() {
        // Inti aturan ini. BmkgMapper menghitung weatherCode harian sebagai
        // kode DOMINAN siang hari, jadi badai tiga jam pada hari cerah tidak
        // akan pernah muncul di kode harian — padahal justru itu yang ingin
        // diperingatkan.
        val forecast = prakiraan(
            hari(
                code = 1,
                hourly = listOf(jam(7, 1), jam(10, 1), jam(13, 95), jam(16, 1)),
            ),
        )

        val alert = ExtremeWeatherRule.evaluate(forecast)

        assertEquals(AlertReason.THUNDERSTORM, alert?.reason)
        assertEquals("Keude Bakongan", alert?.regionName)
        assertEquals(hariIni, alert?.date)
    }

    @Test
    fun `hari cerah tidak memicu peringatan`() {
        val forecast = prakiraan(hari(code = 1, rain = 0.0, hourly = listOf(jam(10, 1))))

        assertNull(ExtremeWeatherRule.evaluate(forecast))
    }

    @Test
    fun `hujan tepat di ambang memicu peringatan`() {
        val forecast = prakiraan(hari(rain = Constants.EXTREME_RAIN_MM_PER_DAY))

        assertEquals(AlertReason.HEAVY_RAIN, ExtremeWeatherRule.evaluate(forecast)?.reason)
    }

    @Test
    fun `hujan sedikit di bawah ambang tidak memicu`() {
        val forecast = prakiraan(hari(rain = Constants.EXTREME_RAIN_MM_PER_DAY - 0.1))

        assertNull(ExtremeWeatherRule.evaluate(forecast))
    }

    @Test
    fun `hujan lokal biasa tidak dianggap ekstrem`() {
        // Kode 80-81 lazim hampir setiap hari di Indonesia; menganggapnya
        // ekstrem akan membuat notifikasi berbunyi terus dan diabaikan.
        val forecast = prakiraan(hari(code = 80, hourly = listOf(jam(10, 80), jam(13, 81))))

        assertNull(ExtremeWeatherRule.evaluate(forecast))
    }

    @Test
    fun `hujan sangat lebat kode 82 dianggap ekstrem`() {
        val forecast = prakiraan(hari(code = 1, hourly = listOf(jam(13, 82))))

        assertEquals(AlertReason.HEAVY_RAIN, ExtremeWeatherRule.evaluate(forecast)?.reason)
    }

    @Test
    fun `curah hujan null tidak membuat crash`() {
        val forecast = prakiraan(hari(rain = null, hourly = listOf(jam(10, 1))))

        assertNull(ExtremeWeatherRule.evaluate(forecast))
    }

    @Test
    fun `prakiraan kosong aman`() {
        assertNull(ExtremeWeatherRule.evaluate(prakiraan()))
    }

    @Test
    fun `kode cuaca tak dikenal tidak membuat crash`() {
        // BMKG bisa memperkenalkan kode baru kapan saja; itu tidak boleh
        // menjatuhkan pemeriksaan latar belakang.
        val forecast = prakiraan(hari(code = 200, hourly = listOf(jam(10, 200))))

        assertNull(ExtremeWeatherRule.evaluate(forecast))
    }

    @Test
    fun `badai di luar jangkauan hari tidak memicu`() {
        // Hanya hari-hari awal yang benar-benar dari BMKG. Hari jauh adalah
        // estimasi Open-Meteo dan tidak layak membangunkan siapa pun.
        val aman = hari(code = 1, hourly = listOf(jam(10, 1)))
        val badai = hari(date = hariIni.plusDays(4), hourly = listOf(jam(10, 95)))

        val forecast = prakiraan(aman, aman, aman, aman, badai)

        assertNull(ExtremeWeatherRule.evaluate(forecast))
    }

    @Test
    fun `hanya satu peringatan dikembalikan meski dua hari sama-sama buruk`() {
        val badaiHariIni = hari(hourly = listOf(jam(10, 95)))
        val badaiBesok = hari(date = hariIni.plusDays(1), hourly = listOf(jam(10, 97)))

        val alert = ExtremeWeatherRule.evaluate(prakiraan(badaiHariIni, badaiBesok))

        // Yang pertama, yaitu yang paling dekat waktunya.
        assertEquals(hariIni, alert?.date)
    }

    @Test
    fun `tanpa rincian per jam kode harian tetap dipakai`() {
        // Hari dari Open-Meteo tidak punya daftar hourly sama sekali.
        val forecast = prakiraan(hari(code = 95, hourly = emptyList()))

        assertEquals(AlertReason.THUNDERSTORM, ExtremeWeatherRule.evaluate(forecast)?.reason)
    }
}
