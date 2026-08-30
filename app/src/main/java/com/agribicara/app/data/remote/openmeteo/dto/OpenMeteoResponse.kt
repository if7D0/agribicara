package com.agribicara.app.data.remote.openmeteo.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response Open-Meteo, dipetakan dari panggilan nyata (spike 2026-08-29).
 *
 * Bentuknya KOLOMNAR — bukan array objek seperti BMKG, melainkan beberapa
 * array paralel yang berpasangan lewat indeks:
 *
 * ```
 * "daily": { "time": ["2026-08-29", ...], "temperature_2m_max": [30.9, ...] }
 * ```
 *
 * Karena itu BMKG dan Open-Meteo tidak bisa berbagi DTO.
 *
 * Catatan: [latitude]/[longitude] yang dikembalikan DIGESER ke titik grid
 * terdekat (kirim -6.9080, balas -6.9244). Itu perilaku normal, bukan error.
 */
@Serializable
data class OpenMeteoResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val elevation: Double? = null,
    val daily: OpenMeteoDailyDto? = null,
)

@Serializable
data class OpenMeteoDailyDto(
    /** Tanggal ISO "yyyy-MM-dd", sudah dalam zona waktu yang diminta. */
    val time: List<String> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerialName("precipitation_sum") val precipitationSum: List<Double?> = emptyList(),
    @SerialName("windspeed_10m_max") val windSpeedMax: List<Double?> = emptyList(),
    /** Kode WMO — standar yang sama dengan field `weather` milik BMKG. */
    @SerialName("weathercode") val weatherCode: List<Int?> = emptyList(),
)
