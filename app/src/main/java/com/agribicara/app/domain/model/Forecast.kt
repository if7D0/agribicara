package com.agribicara.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Model domain cuaca.
 *
 * Paket `domain/` tidak boleh mengimpor apa pun dari `data/` — DTO tinggal di
 * data layer dan dipetakan ke model ini. Bentuk yang ditetapkan di sini
 * menjadi acuan untuk Fase 3-5.
 */
data class Forecast(
    val regionCode: String,
    val regionName: String,
    val days: List<DailyForecast>,
    /** Sumber dominan data yang ditampilkan; menentukan banner/badge di UI. */
    val source: WeatherSource,
    /** Kapan data ini diambil — dipakai untuk menentukan kebasian cache. */
    val fetchedAt: Long,
)

data class DailyForecast(
    val date: LocalDate,
    val temperatureMax: Double?,
    val temperatureMin: Double?,
    val precipitationMm: Double?,
    val windSpeed: Double?,
    /** Kode WMO. Standar sama untuk BMKG maupun Open-Meteo. */
    val weatherCode: Int?,
    /**
     * Deskripsi Bahasa Indonesia. BMKG menyediakannya langsung; untuk
     * Open-Meteo nilainya null dan UI memakai [weatherCode] lewat mapper.
     */
    val description: String?,
    val source: WeatherSource,
    /** Rincian per 3 jam. Hanya terisi untuk hari yang berasal dari BMKG. */
    val hourly: List<HourlyForecast> = emptyList(),
)

data class HourlyForecast(
    val time: LocalDateTime,
    val temperatureCelsius: Double?,
    val humidityPercent: Double?,
    val precipitationMm: Double?,
    val windSpeed: Double?,
    val weatherCode: Int?,
    val description: String?,
)

/**
 * Asal data satu hari prakiraan.
 *
 * Dibedakan karena berpengaruh langsung ke UI: [OPEN_METEO] diberi badge
 * "Data estimasi", dan [CACHE] memunculkan banner mode offline.
 */
enum class WeatherSource {
    /** BMKG — hiperlokal level kelurahan, hanya tersedia ~3 hari ke depan. */
    BMKG,

    /** Open-Meteo — menambal hari 4-7 atau menggantikan BMKG saat gagal. */
    OPEN_METEO,

    /** Dibaca dari Room karena seluruh jaringan gagal. */
    CACHE,
}
