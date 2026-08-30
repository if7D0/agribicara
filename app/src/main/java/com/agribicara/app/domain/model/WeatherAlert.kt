package com.agribicara.app.domain.model

import java.time.LocalDate

/**
 * Peringatan cuaca ekstrem yang layak mengganggu petani (Fase 6).
 *
 * Sengaja membawa [date] dan [regionName], bukan hanya teks jadi: penyusunan
 * kalimatnya adalah urusan lapisan presentasi yang punya akses ke resource
 * string, sedangkan paket `domain/` tidak boleh mengenal Android sama sekali.
 */
data class WeatherAlert(
    val regionName: String,
    val date: LocalDate,
    val reason: AlertReason,
)

/**
 * Kenapa satu hari dianggap ekstrem.
 *
 * Dibedakan karena kalimat peringatannya berbeda, dan petani perlu tahu apa
 * yang harus disiapkan — badai dan hujan lebat menuntut tindakan berbeda.
 */
enum class AlertReason {
    /** Kode WMO badai petir. */
    THUNDERSTORM,

    /** Curah hujan harian melewati ambang, atau kode hujan sangat lebat. */
    HEAVY_RAIN,
}
