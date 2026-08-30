package com.agribicara.app.core.util

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.agribicara.app.R

/**
 * Pemetaan kode cuaca WMO ke ikon dan label.
 *
 * BMKG dan Open-Meteo sama-sama memakai standar kode WMO (dikonfirmasi pada
 * spike 2026-08-29: BMKG `weather:61` berpasangan dengan `"Hujan Ringan"`,
 * persis arti WMO 61). Karena itu SATU mapper melayani kedua sumber — jangan
 * membuat mapper terpisah per sumber.
 *
 * Untuk data BMKG, `weather_desc` bawaan API lebih disukai daripada
 * [labelFor] karena sudah Bahasa Indonesia dan mengikuti istilah resmi BMKG.
 * Mapper ini menjadi sumber label untuk Open-Meteo yang tidak mengirim
 * deskripsi apa pun.
 */
object WeatherCodeMapper {

    /**
     * Ikon untuk satu kode WMO.
     *
     * Kode tak dikenal WAJIB jatuh ke ikon default, bukan melempar exception:
     * BMKG bisa memperkenalkan kode baru kapan saja dan itu tidak boleh
     * membuat layar cuaca gagal render.
     */
    fun iconFor(code: Int?): ImageVector = when (code) {
        // 0 dan 1 sama-sama "Cerah" di BMKG — dua-duanya harus ditangani.
        0, 1 -> Icons.Filled.WbSunny
        2 -> Icons.Filled.WbCloudy
        3 -> Icons.Filled.Cloud
        in 45..48 -> Icons.Filled.BlurOn
        in 51..57 -> Icons.Filled.Grain
        in 61..67 -> Icons.Filled.WaterDrop
        in 71..77 -> Icons.Filled.AcUnit
        in 80..82 -> Icons.Filled.WaterDrop
        in 85..86 -> Icons.Filled.AcUnit
        in 95..99 -> Icons.Filled.Thunderstorm
        else -> Icons.Filled.HelpOutline
    }

    /** Label Bahasa Indonesia untuk satu kode WMO. */
    @StringRes
    fun labelFor(code: Int?): Int = when (code) {
        0, 1 -> R.string.weather_clear
        2 -> R.string.weather_partly_cloudy
        3 -> R.string.weather_cloudy
        in 45..48 -> R.string.weather_fog
        in 51..57 -> R.string.weather_drizzle
        in 61..67 -> R.string.weather_rain
        in 71..77 -> R.string.weather_snow
        in 80..82 -> R.string.weather_rain_showers
        in 85..86 -> R.string.weather_snow
        in 95..99 -> R.string.weather_thunderstorm
        else -> R.string.weather_unknown
    }
}
