package com.agribicara.app.data.remote.bmkg.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bentuk response BMKG, dipetakan dari panggilan nyata (spike 2026-08-29).
 *
 * ```
 * { "lokasi": {...},
 *   "data": [ { "lokasi": {...}, "cuaca": [ [hari-1], [hari-2], [hari-3] ] } ] }
 * ```
 *
 * SEMUA field nullable dengan default. BMKG bisa menambah/menghapus field
 * kapan saja dan itu tidak boleh meruntuhkan parsing — ini bagian dari
 * mitigasi risiko "format berubah" di PRD.
 */
@Serializable
data class BmkgForecastResponse(
    val lokasi: BmkgLocationDto? = null,
    @SerialName("data") val forecasts: List<BmkgForecastGroupDto> = emptyList(),
)

@Serializable
data class BmkgForecastGroupDto(
    val lokasi: BmkgLocationDto? = null,
    /**
     * Array BERSARANG DUA TINGKAT: satu elemen luar per hari lokal, isinya
     * entri per 3 jam. Grup pertama biasanya PARSIAL (dimulai dari jam
     * sekarang, bukan 00:00) — jangan asumsikan panjangnya tetap.
     */
    val cuaca: List<List<BmkgWeatherDto>> = emptyList(),
)

@Serializable
data class BmkgLocationDto(
    val adm1: String? = null,
    val adm2: String? = null,
    val adm3: String? = null,
    val adm4: String? = null,
    val provinsi: String? = null,
    val kotkab: String? = null,
    val kecamatan: String? = null,
    val desa: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    /**
     * HATI-HATI: pada objek terluar berisi ZoneId valid ("Asia/Jakarta"),
     * tapi pada objek di dalam `data[]` berisi offset ("+0700"). Jangan
     * pernah memberikan nilai ini ke ZoneId.of() tanpa pengecekan.
     */
    val timezone: String? = null,
)

@Serializable
data class BmkgWeatherDto(
    /**
     * Waktu LOKAL, format "yyyy-MM-dd HH:mm:ss". Inilah yang dipakai untuk
     * mengelompokkan hari — bukan [datetime] yang UTC dan akan menggeser
     * batas hari untuk entri malam.
     */
    @SerialName("local_datetime") val localDatetime: String? = null,
    /** Waktu UTC berformat ISO ("2026-08-29T10:00:00Z"). */
    val datetime: String? = null,
    /** Suhu udara, derajat Celsius. */
    @SerialName("t") val temperatureCelsius: Double? = null,
    /** Tutupan awan total, persen. */
    @SerialName("tcc") val cloudCoverPercent: Double? = null,
    /** Curah hujan, milimeter. */
    @SerialName("tp") val precipitationMm: Double? = null,
    /** Kelembapan relatif, persen. */
    @SerialName("hu") val humidityPercent: Double? = null,
    /** Kecepatan angin. */
    @SerialName("ws") val windSpeed: Double? = null,
    /** Arah angin asal, mis. "SE". */
    @SerialName("wd") val windDirection: String? = null,
    /** Jarak pandang, meter. */
    @SerialName("vs") val visibilityMeters: Double? = null,
    /** Kode cuaca WMO — SAMA standarnya dengan Open-Meteo. */
    @SerialName("weather") val weatherCode: Int? = null,
    /** Deskripsi Bahasa Indonesia siap tampil, mis. "Hujan Ringan". */
    @SerialName("weather_desc") val weatherDescription: String? = null,
)

