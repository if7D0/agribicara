package com.agribicara.app.data.mapper

import com.agribicara.app.data.remote.openmeteo.dto.OpenMeteoResponse
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.WeatherSource
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Open-Meteo DTO -> model domain.
 *
 * Response-nya KOLOMNAR: beberapa array paralel yang berpasangan lewat indeks.
 * Panjang array tidak dijamin sama bila response rusak, jadi iterasi dibatasi
 * ke array TERPENDEK — bukan ke `time.size` — supaya tidak ada
 * IndexOutOfBounds pada data yang cacat sebagian.
 *
 * Open-Meteo tidak mengirim deskripsi teks, hanya kode WMO. Karena itu
 * [DailyForecast.description] sengaja null di sini dan UI memakai
 * `WeatherCodeMapper.labelFor` sebagai gantinya.
 */
object OpenMeteoMapper {

    fun toDailyForecasts(response: OpenMeteoResponse): List<DailyForecast> {
        val daily = response.daily ?: return emptyList()
        if (daily.time.isEmpty()) return emptyList()

        // Batas aman: sekecil-kecilnya array yang benar-benar terisi.
        val size = listOf(
            daily.time.size,
            daily.temperatureMax.size.orMax(),
            daily.temperatureMin.size.orMax(),
            daily.precipitationSum.size.orMax(),
            daily.windSpeedMax.size.orMax(),
            daily.weatherCode.size.orMax(),
        ).min()

        return (0 until size).mapNotNull { index ->
            val date = parseDate(daily.time[index]) ?: return@mapNotNull null
            DailyForecast(
                date = date,
                temperatureMax = daily.temperatureMax.getOrNull(index),
                temperatureMin = daily.temperatureMin.getOrNull(index),
                precipitationMm = daily.precipitationSum.getOrNull(index),
                windSpeed = daily.windSpeedMax.getOrNull(index),
                weatherCode = daily.weatherCode.getOrNull(index),
                description = null,
                source = WeatherSource.OPEN_METEO,
            )
        }
    }

    /**
     * Array yang kosong sama sekali diperlakukan sebagai "tidak membatasi".
     * Tanpa ini, satu field opsional yang tidak dikirim server akan memangkas
     * seluruh prakiraan menjadi nol hari.
     */
    private fun Int.orMax(): Int = if (this == 0) Int.MAX_VALUE else this

    private fun parseDate(raw: String): LocalDate? = try {
        LocalDate.parse(raw.trim())
    } catch (e: DateTimeParseException) {
        null
    }
}
