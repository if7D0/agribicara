package com.agribicara.app.data.mapper

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.DailyForecast
import java.time.LocalDate

/**
 * Menggabungkan dua sumber menjadi satu prakiraan [Constants.FORECAST_DAYS] hari.
 *
 * Alasan keberadaannya: BMKG hanya menyediakan ~3 hari (terverifikasi pada
 * spike), sedangkan success signal PRD menuntut 7 hari. BMKG dipakai untuk
 * hari-hari yang dia punya karena datanya hiperlokal (level kelurahan) dan
 * berdeskripsi Bahasa Indonesia resmi; sisanya ditambal Open-Meteo dan
 * ditandai sebagai estimasi di UI.
 *
 * BMKG menang untuk deskripsi, kode cuaca, dan rincian per jam. Suhu ekstrem
 * adalah pengecualian — lihat [mergeTemperatureExtremes].
 */
object ForecastMerger {

    fun merge(
        primary: List<DailyForecast>,
        secondary: List<DailyForecast>,
        today: LocalDate,
        limit: Int = Constants.FORECAST_DAYS,
    ): List<DailyForecast> {
        val secondaryByDate = secondary.associateBy { it.date }
        val byDate = LinkedHashMap<LocalDate, DailyForecast>()

        // Urutan penyisipan menentukan pemenang: primary dulu, lalu secondary
        // hanya mengisi tanggal yang belum ada.
        primary.forEach { day ->
            byDate.putIfAbsent(day.date, mergeTemperatureExtremes(day, secondaryByDate[day.date]))
        }
        secondary.forEach { byDate.putIfAbsent(it.date, it) }

        return byDate.values
            // Tanggal yang sudah lewat tidak berguna bagi petani dan bisa
            // muncul dari cache lama atau dari entri parsial hari ini.
            .filter { !it.date.isBefore(today) }
            .sortedBy { it.date }
            .take(limit)
    }

    /**
     * Jam paling awal yang harus tercakup agar satu hari dianggap lengkap.
     *
     * Suhu puncak harian di Indonesia terjadi sekitar tengah hari, jadi hari
     * yang entri pertamanya sudah lewat jam ini dipastikan kehilangan
     * puncaknya.
     */
    private const val COMPLETE_DAY_START_HOUR = 9

    /**
     * Melebarkan rentang suhu HANYA untuk hari BMKG yang datanya parsial.
     *
     * Kenapa perlu: grup hari pertama BMKG dimulai dari jam sekarang, bukan
     * 00:00. Membuka app pukul 17:00 berarti suhu maksimum "hari ini" hanya
     * dihitung dari entri sore dan malam, sehingga kartu Home menampilkan 25°
     * padahal puncak hari itu 31° — justru pada jam ketika petani memeriksa
     * sebelum memutuskan menyemprot.
     *
     * Kenapa HANYA untuk hari parsial: pada hari yang lengkap, pengukuran
     * BMKG level kelurahan lebih dipercaya daripada estimasi grid Open-Meteo.
     * Menggabung di situ akan mencemari data yang lebih baik dengan yang lebih
     * kasar, dan menghapus justru keunggulan yang membuat BMKG dipilih.
     *
     * Yang digabung HANYA suhu maksimum/minimum. Deskripsi, kode cuaca, curah
     * hujan, dan rincian per jam tetap milik BMKG supaya teks dan ikon tidak
     * saling bertentangan.
     */
    private fun mergeTemperatureExtremes(
        primary: DailyForecast,
        secondary: DailyForecast?,
    ): DailyForecast {
        if (secondary == null || !primary.isPartialDay()) return primary

        val max = maxOfNullable(primary.temperatureMax, secondary.temperatureMax)
        val min = minOfNullable(primary.temperatureMin, secondary.temperatureMin)
        if (max == primary.temperatureMax && min == primary.temperatureMin) return primary

        return primary.copy(temperatureMax = max, temperatureMin = min)
    }

    /**
     * Hari yang rincian per jamnya tidak mencakup pagi.
     *
     * Hari tanpa rincian per jam sama sekali (mis. dibaca dari cache) TIDAK
     * dianggap parsial — kita tidak punya dasar untuk menilainya, dan menebak
     * lebih berbahaya daripada membiarkan.
     */
    private fun DailyForecast.isPartialDay(): Boolean {
        val earliestHour = hourly.minOfOrNull { it.time.hour } ?: return false
        return earliestHour > COMPLETE_DAY_START_HOUR
    }

    private fun maxOfNullable(a: Double?, b: Double?): Double? = when {
        a == null -> b
        b == null -> a
        else -> maxOf(a, b)
    }

    private fun minOfNullable(a: Double?, b: Double?): Double? = when {
        a == null -> b
        b == null -> a
        else -> minOf(a, b)
    }
}
