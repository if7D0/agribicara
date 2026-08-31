package com.agribicara.app.data.notification

import com.agribicara.app.core.common.Constants
import com.agribicara.app.data.local.dao.SentAlertDao
import com.agribicara.app.data.local.entity.SentAlertEntity
import com.agribicara.app.domain.model.WeatherAlert
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengingat peringatan cuaca terakhir supaya tidak diulang.
 *
 * Tanpa ini, satu badai yang diramalkan untuk besok akan diberitahukan sampai
 * empat kali sehari: pemeriksaannya berjalan tiap
 * [Constants.WEATHER_CHECK_INTERVAL_HOURS] jam sementara jangkauannya
 * [Constants.ALERT_LOOKAHEAD_DAYS] hari, jadi kejadian yang sama bertahan di
 * prakiraan sepanjang hari.
 *
 * Ini kegagalan yang persis diperingatkan KDoc
 * [com.agribicara.app.domain.weather.ExtremeWeatherRule] — notifikasi yang
 * mengganggu berulang kali akan dimatikan petani, dan setelah dimatikan
 * peringatan yang benar pun tidak akan pernah sampai lagi. Bedanya di sini
 * bukan peringatan palsu, melainkan peringatan benar yang diulang-ulang.
 *
 * `setOnlyAlertOnce(true)` SENGAJA tidak dipakai sebagai gantinya. Seluruh
 * peringatan cuaca berbagi satu id notifikasi
 * ([Constants.WEATHER_ALERT_NOTIFICATION_ID]), sehingga flag itu justru akan
 * membisukan peringatan BARU yang menggantikan peringatan lama yang masih
 * tampil di panel — membungkam yang benar sambil membiarkan pengulangannya.
 */
@Singleton
class AlertHistory @Inject constructor(
    private val dao: SentAlertDao,
    private val clock: Clock,
) {

    /**
     * True bila [alert] belum pernah ditampilkan.
     *
     * Yang dibandingkan wilayah + tanggal + alasan. Perubahan salah satunya
     * adalah kejadian berbeda dan layak mengganggu lagi: pindah wilayah,
     * hari yang berbeda, atau badai yang berubah menjadi hujan lebat.
     *
     * Nama wilayah SENGAJA tidak ikut dibandingkan — ejaannya bisa berbeda
     * antara BMKG dan Open-Meteo, dan perbedaan ejaan bukan kejadian baru.
     *
     * Catatan yang disengaja: peringatan "badai besok" yang keesokan harinya
     * menjadi "badai hari ini" TIDAK dikirim ulang, karena tanggalnya sama.
     * Petani sudah diberi tahu tentang kejadian itu, dan memberitahukannya
     * lagi hanya karena labelnya berubah adalah pengulangan yang sama saja.
     */
    suspend fun belumPernahDikirim(alert: WeatherAlert): Boolean {
        val terakhir = dao.get() ?: return true
        return terakhir.regionCode != alert.regionCode ||
            terakhir.date != alert.date.toString() ||
            terakhir.reason != alert.reason.name
    }

    /**
     * Mencatat [alert] sebagai sudah ditampilkan.
     *
     * Dipanggil HANYA setelah notifikasinya benar-benar tampil. Mencatat lebih
     * awal berarti peringatan yang gagal tampil — misalnya karena izin belum
     * diberikan — akan dianggap sudah tersampaikan dan tidak pernah dicoba
     * lagi meski izinnya kemudian diberikan.
     */
    suspend fun catat(alert: WeatherAlert) {
        dao.upsert(
            SentAlertEntity(
                regionCode = alert.regionCode,
                date = alert.date.toString(),
                reason = alert.reason.name,
                notifiedAt = clock.millis(),
            ),
        )
    }
}
