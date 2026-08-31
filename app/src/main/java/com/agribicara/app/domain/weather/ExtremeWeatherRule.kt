package com.agribicara.app.domain.weather

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.AlertReason
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.HourlyForecast
import com.agribicara.app.domain.model.WeatherAlert
import java.time.LocalDateTime

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
     *
     * [now] WAJIB diteruskan, bukan dibaca dari jam sistem di dalam sini —
     * dua alasan. Pertama, objek ini harus tetap bisa diuji tanpa memanipulasi
     * waktu global. Kedua, dan lebih penting: tanpa waktu sekarang, aturan ini
     * tidak punya cara membedakan cuaca yang AKAN datang dari cuaca yang SUDAH
     * lewat, dan pernah benar-benar memperingatkan badai yang sudah berakhir
     * sebelas jam sebelumnya.
     */
    fun evaluate(forecast: Forecast, now: LocalDateTime): WeatherAlert? {
        val today = now.toLocalDate()

        return forecast.days
            // Disaring SEBELUM take(): satu hari basi yang lolos dari cache
            // tidak boleh menghabiskan jatah lookahead dan menutupi badai
            // besok. WeatherRepositoryImpl sudah menyaring per tanggal, ini
            // lapis kedua yang murah.
            .filter { !it.date.isBefore(today) }
            .take(Constants.ALERT_LOOKAHEAD_DAYS)
            .firstNotNullOfOrNull { day ->
                reasonFor(day, now)?.let { reason ->
                    WeatherAlert(
                        regionCode = forecast.regionCode,
                        regionName = forecast.regionName,
                        date = day.date,
                        reason = reason,
                    )
                }
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
     * (contoh nilai 1,5-10,6 — konsisten dengan km/jam maupun m/s), sedangkan
     * Open-Meteo mengirim km/jam. Satu ambang di atas dua satuan yang mungkin
     * berbeda akan menghasilkan alarm palsu terus-menerus atau diam total.
     * Jangan menambahkannya sebelum satuannya benar-benar diverifikasi.
     */
    private fun reasonFor(day: DailyForecast, now: LocalDateTime): AlertReason? {
        val sisaHari = day.hourly.filter { it.belumSelesai(now) }

        val codes = sisaHari.mapNotNull { it.weatherCode }
            .ifEmpty { kodeTingkatHari(day, now) }

        if (codes.any { it in THUNDERSTORM_CODES }) return AlertReason.THUNDERSTORM

        if (codes.any { it == VIOLENT_SHOWER_CODE }) return AlertReason.HEAVY_RAIN

        val rain = curahHujanYangBelumTurun(day, sisaHari, now)
        if (rain != null && rain >= Constants.EXTREME_RAIN_MM_PER_DAY) {
            return AlertReason.HEAVY_RAIN
        }

        return null
    }

    /**
     * Slot 3-jam-an dianggap masih relevan selama SEBAGIAN saja belum lewat.
     *
     * Batasnya akhir slot, bukan awalnya: badai yang mulai pukul 18.00 masih
     * berlangsung pukul 19.30, dan membuang slotnya berarti membuang
     * peringatan yang justru sedang berlaku. Sebaliknya slot pukul 07.00 pada
     * pemeriksaan pukul 18.00 memang harus hilang — itulah cacat yang
     * memunculkan notifikasi "badai diperkirakan hari ini" untuk badai yang
     * sudah berakhir.
     *
     * Untuk hari-hari mendatang penyaringan ini tidak membuang apa pun; yang
     * benar-benar tersaring hanya hari ini.
     */
    private fun HourlyForecast.belumSelesai(now: LocalDateTime): Boolean =
        time.plusHours(Constants.FORECAST_SLOT_HOURS).isAfter(now)

    /**
     * Sinyal tingkat-hari ([DailyForecast.weatherCode]) hanya dipakai untuk
     * hari yang SELURUHNYA masih di depan.
     *
     * Hari dari Open-Meteo tidak punya rincian per jam sama sekali, jadi tanpa
     * jalur ini hari ke-2 tidak akan pernah bisa diperiksa saat BMKG mati.
     * Tetapi untuk HARI INI nilai itu merangkum seluruh hari termasuk jam yang
     * sudah lewat, sehingga memakainya berarti mengulang cacat yang sama lewat
     * pintu belakang.
     *
     * Konsekuensinya dicatat terbuka, bukan disembunyikan: bila BMKG mati DAN
     * cuaca ekstremnya jatuh pada hari ini, peringatan itu hilang. Yang
     * ditukar adalah kemungkinan kehilangan satu peringatan melawan
     * kepastian mengirim peringatan yang sudah tidak benar — dan peringatan
     * yang tidak benar adalah yang membuat notifikasi dimatikan selamanya.
     */
    private fun kodeTingkatHari(day: DailyForecast, now: LocalDateTime): List<Int> =
        if (day.date.isAfter(now.toLocalDate())) listOfNotNull(day.weatherCode) else emptyList()

    /**
     * Hujan yang masih AKAN turun, bukan total sehari penuh.
     *
     * `BmkgMapper` menjumlahkan nilai harian dari seluruh slot, termasuk hujan
     * yang sudah turun pagi tadi. Menjumlahkan ulang hanya dari slot yang
     * tersisa memberi angka yang benar-benar bisa ditindaklanjuti, dan untuk
     * hari mendatang hasilnya identik dengan nilai harian karena tidak ada
     * slot yang terbuang.
     */
    private fun curahHujanYangBelumTurun(
        day: DailyForecast,
        sisaHari: List<HourlyForecast>,
        now: LocalDateTime,
    ): Double? = when {
        day.hourly.isNotEmpty() ->
            sisaHari.mapNotNull { it.precipitationMm }.takeIf { it.isNotEmpty() }?.sum()

        day.date.isAfter(now.toLocalDate()) -> day.precipitationMm

        else -> null
    }
}
