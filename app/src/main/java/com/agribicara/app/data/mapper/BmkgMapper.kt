package com.agribicara.app.data.mapper

import com.agribicara.app.data.remote.bmkg.dto.BmkgForecastResponse
import com.agribicara.app.data.remote.bmkg.dto.BmkgWeatherDto
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.HourlyForecast
import com.agribicara.app.domain.model.WeatherSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * BMKG DTO -> model domain.
 *
 * Dua jebakan yang ditangani di sini, keduanya ditemukan lewat spike:
 *
 * 1. Array `cuaca` bersarang dua tingkat dan grup pertamanya PARSIAL
 *    (mulai dari jam sekarang, bukan 00:00). Karena itu pengelompokan hari
 *    TIDAK boleh mengandalkan indeks atau panjang tetap — kita ratakan
 *    seluruh entri lalu kelompokkan ulang berdasarkan tanggal sungguhan.
 *
 * 2. Pengelompokan memakai `local_datetime`, bukan `datetime` yang UTC.
 *    Entri pukul 23:00 WIB adalah 16:00 UTC di hari yang sama, tapi entri
 *    pukul 02:00 WIB adalah 19:00 UTC di hari SEBELUMNYA — memakai UTC akan
 *    menggeser entri dini hari ke hari yang salah.
 */
object BmkgMapper {

    private val LOCAL_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** Rentang jam yang dianggap mewakili "cuaca hari itu" bagi petani. */
    private val DAYTIME_HOURS = 6..18

    fun toDailyForecasts(response: BmkgForecastResponse): List<DailyForecast> {
        val entries = response.forecasts
            .flatMap { group -> group.cuaca.flatten() }
            .mapNotNull { dto -> dto.toHourly() }

        if (entries.isEmpty()) return emptyList()

        return entries
            .groupBy { it.time.toLocalDate() }
            .toSortedMap()
            .map { (date, hourly) -> summarize(date, hourly) }
    }

    /** Nama wilayah terlengkap yang tersedia, untuk ditampilkan di judul. */
    fun regionDisplayName(response: BmkgForecastResponse): String? {
        val lokasi = response.lokasi ?: response.forecasts.firstOrNull()?.lokasi ?: return null
        val desa = lokasi.desa?.takeIf { it.isNotBlank() }
        val kecamatan = lokasi.kecamatan?.takeIf { it.isNotBlank() }
        return listOfNotNull(desa, kecamatan).takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    private fun summarize(date: LocalDate, hourly: List<HourlyForecast>): DailyForecast {
        // Kode cuaca yang mewakili hari itu diambil dari jam siang bila ada.
        // Cuaca pukul 02:00 tidak berguna untuk memutuskan kapan menyemprot.
        val representative = hourly.filter { it.time.hour in DAYTIME_HOURS }
            .ifEmpty { hourly }
        val dominant = representative
            .mapNotNull { it.weatherCode }
            .groupingBy { it }
            .eachCount()
            .entries
            // Terbanyak menang; seri dimenangkan kode tertinggi karena kode
            // WMO yang lebih besar berarti cuaca lebih berat (hujan > cerah),
            // dan salah-peringatan lebih aman daripada salah-aman.
            .maxWithOrNull(compareBy({ it.value }, { it.key }))
            ?.key

        return DailyForecast(
            date = date,
            temperatureMax = hourly.mapNotNull { it.temperatureCelsius }.maxOrNull(),
            temperatureMin = hourly.mapNotNull { it.temperatureCelsius }.minOrNull(),
            precipitationMm = hourly.mapNotNull { it.precipitationMm }
                .takeIf { it.isNotEmpty() }?.sum(),
            windSpeed = hourly.mapNotNull { it.windSpeed }.maxOrNull(),
            weatherCode = dominant,
            description = representative.firstOrNull { it.weatherCode == dominant }?.description,
            source = WeatherSource.BMKG,
            hourly = hourly,
        )
    }

    private fun BmkgWeatherDto.toHourly(): HourlyForecast? {
        val parsed = localDatetime?.let(::parseLocal) ?: return null
        return HourlyForecast(
            time = parsed,
            temperatureCelsius = temperatureCelsius,
            humidityPercent = humidityPercent,
            precipitationMm = precipitationMm,
            windSpeed = windSpeed,
            weatherCode = weatherCode,
            description = weatherDescription?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * Entri dengan waktu tak terbaca dibuang, bukan membuat seluruh response
     * gagal: satu baris rusak tidak boleh menghapus prakiraan enam hari lain.
     */
    private fun parseLocal(raw: String): LocalDateTime? = try {
        LocalDateTime.parse(raw.trim(), LOCAL_FORMATTER)
    } catch (e: DateTimeParseException) {
        null
    }
}
