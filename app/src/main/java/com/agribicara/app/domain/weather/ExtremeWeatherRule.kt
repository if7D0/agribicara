package com.agribicara.app.domain.weather

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.AlertReason
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.WeatherAlert

/**
 * Menentukan apakah prakiraan layak membangunkan notifikasi.
 *
 * Objek murni tanpa Android, seperti [com.agribicara.app.core.util.WeatherCodeMapper]:
 * inilah satu-satunya tempat kebijakan "seberapa buruk baru disebut ekstrem"
 * berada, dan justru karena itu ia harus bisa diuji tanpa perangkat.
 *
 * Ambangnya sengaja konservatif. Peringatan palsu yang berulang membuat petani
 * mematikan notifikasi, dan setelah itu peringatan yang benar pun tidak akan
 * pernah sampai.
 */
object ExtremeWeatherRule {

    /** Kode WMO badai petir. */
    private val THUNDERSTORM_CODES = 95..99

    /**
     * Hujan sangat lebat (violent rain showers).
     *
     * 80..81 (hujan lokal ringan/sedang) TIDAK termasuk: keduanya lazim di
     * Indonesia hampir setiap hari dan akan membuat notifikasi jadi bising.
     */
    private const val VIOLENT_SHOWER_CODE = 82

    /**
     * Peringatan pertama yang ditemukan, atau null bila tidak ada.
     *
     * Hanya satu yang dikembalikan: dua notifikasi berturut-turut untuk dua
     * hari yang sama-sama buruk tidak menambah apa pun yang bisa
     * ditindaklanjuti petani.
     */
    fun evaluate(forecast: Forecast): WeatherAlert? =
        forecast.days
            .take(Constants.ALERT_LOOKAHEAD_DAYS)
            .firstNotNullOfOrNull { day ->
                reasonFor(day)?.let { reason ->
                    WeatherAlert(
                        regionName = forecast.regionName,
                        date = day.date,
                        reason = reason,
                    )
                }
            }

    /**
     * Kode cuaca diperiksa dari daftar [DailyForecast.hourly], BUKAN dari
     * [DailyForecast.weatherCode].
     *
     * Alasannya ada di `data/mapper/BmkgMapper`: kode harian dihitung sebagai
     * kode yang paling sering muncul di siang hari. Badai tiga jam pada hari
     * yang umumnya cerah tidak akan pernah menjadi kode harian — padahal
     * justru kejadian itulah yang ingin diperingatkan.
     *
     * Kecepatan angin SENGAJA TIDAK dipakai. Satuan field `ws` BMKG tidak
     * terdokumentasi di mana pun dan tidak pernah ditampilkan di layar
     * (contoh nilai 1,5–10,6 — konsisten dengan km/jam maupun m/s), sedangkan
     * Open-Meteo mengirim km/jam. Satu ambang di atas dua satuan yang mungkin
     * berbeda akan menghasilkan alarm palsu terus-menerus atau diam total.
     * Jangan menambahkannya sebelum satuannya benar-benar diverifikasi.
     */
    private fun reasonFor(day: DailyForecast): AlertReason? {
        val codes = day.hourly.mapNotNull { it.weatherCode }
            .ifEmpty { listOfNotNull(day.weatherCode) }

        if (codes.any { it in THUNDERSTORM_CODES }) return AlertReason.THUNDERSTORM

        if (codes.any { it == VIOLENT_SHOWER_CODE }) return AlertReason.HEAVY_RAIN

        // Nilai harian adalah JUMLAH slot 3-jam-an (lihat BmkgMapper), jadi
        // sudah setara "mm per hari" yang dipakai ambang BMKG.
        val rain = day.precipitationMm
        if (rain != null && rain >= Constants.EXTREME_RAIN_MM_PER_DAY) {
            return AlertReason.HEAVY_RAIN
        }

        return null
    }
}
