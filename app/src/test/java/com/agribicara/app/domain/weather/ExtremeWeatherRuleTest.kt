package com.agribicara.app.domain.weather

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.AlertReason
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.HourlyForecast
import com.agribicara.app.domain.model.WeatherSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Kebijakan "seberapa buruk baru disebut ekstrem".
 *
 * Dua arah kesalahan sama-sama merugikan dan keduanya diuji: terlalu sensitif
 * membuat petani mematikan notifikasi (setelah itu peringatan yang benar pun
 * tidak sampai), terlalu tumpul membuat fitur ini tidak ada gunanya.
 *
 * Sejak perbaikan H2 review Fase 6, arah ketiga ikut diuji: peringatan untuk
 * cuaca yang SUDAH LEWAT. Aturan ini pernah mengirim "badai diperkirakan hari
 * ini" untuk badai yang berakhir sebelas jam sebelumnya.
 */
class ExtremeWeatherRuleTest {

    private val hariIni = LocalDate.of(2026, 8, 31)
    private val besok = hariIni.plusDays(1)

    /** Pemeriksaan pagi: seluruh hari masih di depan. */
    private val pagi = LocalDateTime.of(hariIni, LocalTime.of(6, 0))

    /** Pemeriksaan sore: slot pagi sudah lewat. */
    private val sore = LocalDateTime.of(hariIni, LocalTime.of(18, 0))

    private fun jam(
        hour: Int,
        code: Int?,
        date: LocalDate = hariIni,
        rain: Double = 0.0,
    ) = HourlyForecast(
        time = LocalDateTime.of(date, LocalTime.of(hour, 0)),
        temperatureCelsius = 28.0,
        humidityPercent = 80.0,
        precipitationMm = rain,
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

        val alert = ExtremeWeatherRule.evaluate(forecast, pagi)

        assertEquals(AlertReason.THUNDERSTORM, alert?.reason)
        assertEquals("Keude Bakongan", alert?.regionName)
        assertEquals("11.01.01.2001", alert?.regionCode)
        assertEquals(hariIni, alert?.date)
    }

    // --- H2: cuaca yang sudah lewat ---------------------------------------

    @Test
    fun `badai yang sudah berakhir tidak lagi diperingatkan`() {
        // Cacat H2 yang sebenarnya terjadi: worker berjalan pukul 18.00, data
        // hari ini masih memuat slot 07.00 berkode 95, dan petani menerima
        // "badai diperkirakan hari ini" untuk badai yang sudah selesai.
        val forecast = prakiraan(hari(code = 1, hourly = listOf(jam(7, 95))))

        assertNull(ExtremeWeatherRule.evaluate(forecast, sore))
    }

    @Test
    fun `badai yang sedang berlangsung tetap diperingatkan`() {
        // Batasnya akhir slot, bukan awalnya. Slot 18.00 masih berjalan pada
        // pukul 19.30; membuangnya berarti membuang peringatan yang sedang
        // berlaku persis ketika paling dibutuhkan.
        val forecast = prakiraan(hari(code = 1, hourly = listOf(jam(18, 95))))
        val setengahJalan = LocalDateTime.of(hariIni, LocalTime.of(19, 30))

        assertEquals(
            AlertReason.THUNDERSTORM,
            ExtremeWeatherRule.evaluate(forecast, setengahJalan)?.reason,
        )
    }

    @Test
    fun `badai malam nanti tetap diperingatkan pada pemeriksaan sore`() {
        val forecast = prakiraan(hari(code = 1, hourly = listOf(jam(7, 1), jam(21, 95))))

        assertEquals(
            AlertReason.THUNDERSTORM,
            ExtremeWeatherRule.evaluate(forecast, sore)?.reason,
        )
    }

    @Test
    fun `hujan yang sudah turun tidak ikut dihitung`() {
        // BmkgMapper menjumlahkan curah hujan SELURUH hari. Pada pukul 18.00
        // sebagian besar sudah turun, dan memperingatkannya sama saja dengan
        // memberi tahu petani tentang hujan yang membasahinya tadi pagi.
        val forecast = prakiraan(
            hari(
                rain = 60.0,
                hourly = listOf(jam(7, 1, rain = 45.0), jam(21, 1, rain = 15.0)),
            ),
        )

        assertNull(ExtremeWeatherRule.evaluate(forecast, sore))
    }

    @Test
    fun `hujan lebat yang belum turun tetap diperingatkan`() {
        val forecast = prakiraan(
            hari(
                rain = 60.0,
                hourly = listOf(jam(7, 1, rain = 5.0), jam(21, 1, rain = 55.0)),
            ),
        )

        assertEquals(
            AlertReason.HEAVY_RAIN,
            ExtremeWeatherRule.evaluate(forecast, sore)?.reason,
        )
    }

    @Test
    fun `hari yang sudah lewat dibuang dan tidak menghabiskan jatah lookahead`() {
        // Penyaringan dilakukan SEBELUM take(): satu hari basi yang lolos dari
        // cache tidak boleh mendorong badai besok keluar dari jangkauan.
        val kemarin = hari(date = hariIni.minusDays(1), hourly = listOf(jam(10, 1)))
        val aman = hari(hourly = listOf(jam(10, 1)))
        val badaiBesok = hari(date = besok, hourly = listOf(jam(10, 95, date = besok)))

        val alert = ExtremeWeatherRule.evaluate(prakiraan(kemarin, aman, badaiBesok), pagi)

        assertEquals(besok, alert?.date)
    }

    // --- ambang dan kekebalan terhadap data aneh ---------------------------

    @Test
    fun `hari cerah tidak memicu peringatan`() {
        val forecast = prakiraan(hari(code = 1, rain = 0.0, hourly = listOf(jam(10, 1))))

        assertNull(ExtremeWeatherRule.evaluate(forecast, pagi))
    }

    @Test
    fun `hujan tepat di ambang memicu peringatan`() {
        val forecast = prakiraan(hari(date = besok, rain = Constants.EXTREME_RAIN_MM_PER_DAY))

        assertEquals(
            AlertReason.HEAVY_RAIN,
            ExtremeWeatherRule.evaluate(forecast, pagi)?.reason,
        )
    }

    @Test
    fun `hujan sedikit di bawah ambang tidak memicu`() {
        val forecast = prakiraan(
            hari(date = besok, rain = Constants.EXTREME_RAIN_MM_PER_DAY - 0.1),
        )

        assertNull(ExtremeWeatherRule.evaluate(forecast, pagi))
    }

    @Test
    fun `hujan lokal biasa tidak dianggap ekstrem`() {
        // Kode 80-81 lazim hampir setiap hari di Indonesia; menganggapnya
        // ekstrem akan membuat notifikasi berbunyi terus dan diabaikan.
        val forecast = prakiraan(hari(code = 80, hourly = listOf(jam(10, 80), jam(13, 81))))

        assertNull(ExtremeWeatherRule.evaluate(forecast, pagi))
    }

    @Test
    fun `hujan sangat lebat kode 82 dianggap ekstrem`() {
        val forecast = prakiraan(hari(code = 1, hourly = listOf(jam(13, 82))))

        assertEquals(
            AlertReason.HEAVY_RAIN,
            ExtremeWeatherRule.evaluate(forecast, pagi)?.reason,
        )
    }

    @Test
    fun `curah hujan null tidak membuat crash`() {
        val forecast = prakiraan(hari(rain = null, hourly = listOf(jam(10, 1))))

        assertNull(ExtremeWeatherRule.evaluate(forecast, pagi))
    }

    @Test
    fun `prakiraan kosong aman`() {
        assertNull(ExtremeWeatherRule.evaluate(prakiraan(), pagi))
    }

    @Test
    fun `kode cuaca tak dikenal tidak membuat crash`() {
        // BMKG bisa memperkenalkan kode baru kapan saja; itu tidak boleh
        // menjatuhkan pemeriksaan latar belakang.
        val forecast = prakiraan(hari(code = 200, hourly = listOf(jam(10, 200))))

        assertNull(ExtremeWeatherRule.evaluate(forecast, pagi))
    }

    @Test
    fun `badai di luar jangkauan hari tidak memicu`() {
        // Hanya hari-hari awal yang benar-benar dari BMKG. Hari jauh adalah
        // estimasi Open-Meteo dan tidak layak membangunkan siapa pun.
        val aman = hari(code = 1, hourly = listOf(jam(10, 1)))
        val amanBesok = hari(date = besok, code = 1, hourly = listOf(jam(10, 1, date = besok)))
        val badai = hari(date = hariIni.plusDays(4), code = 95)

        val forecast = prakiraan(aman, amanBesok, badai)

        assertNull(ExtremeWeatherRule.evaluate(forecast, pagi))
    }

    @Test
    fun `hanya satu peringatan dikembalikan meski dua hari sama-sama buruk`() {
        val badaiHariIni = hari(hourly = listOf(jam(10, 95)))
        val badaiBesok = hari(date = besok, hourly = listOf(jam(10, 97, date = besok)))

        val alert = ExtremeWeatherRule.evaluate(prakiraan(badaiHariIni, badaiBesok), pagi)

        // Yang pertama, yaitu yang paling dekat waktunya.
        assertEquals(hariIni, alert?.date)
    }

    // --- sinyal tingkat-hari (Open-Meteo, tanpa rincian per jam) -----------

    @Test
    fun `hari mendatang tanpa rincian per jam tetap memakai kode harian`() {
        // Hari dari Open-Meteo tidak punya daftar hourly sama sekali. Tanpa
        // jalur ini, hari ke-2 tidak akan pernah bisa diperiksa saat BMKG mati.
        val forecast = prakiraan(hari(date = besok, code = 95, hourly = emptyList()))

        assertEquals(
            AlertReason.THUNDERSTORM,
            ExtremeWeatherRule.evaluate(forecast, pagi)?.reason,
        )
    }

    @Test
    fun `hari ini tanpa rincian per jam sengaja tidak memakai kode harian`() {
        // Keputusan yang dicatat terbuka, bukan cacat. Kode harian merangkum
        // SELURUH hari termasuk jam yang sudah lewat, jadi memakainya untuk
        // hari ini akan mengulang H2 lewat pintu belakang. Harganya: bila BMKG
        // mati DAN cuaca ekstremnya jatuh hari ini, peringatan itu hilang.
        val forecast = prakiraan(hari(code = 95, hourly = emptyList()))

        assertNull(ExtremeWeatherRule.evaluate(forecast, pagi))
    }
}
