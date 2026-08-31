package com.agribicara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agribicara.app.core.common.Constants

/**
 * Peringatan cuaca terakhir yang BENAR-BENAR ditampilkan — tabel baris tunggal
 * (id selalu [Constants.SENT_ALERT_ID]).
 *
 * Ada supaya peringatan yang sama tidak diulang. Pemeriksaan berjalan tiap 6
 * jam sementara jangkauannya 2 hari, jadi badai yang diramalkan untuk besok
 * akan tetap ada di prakiraan sepanjang hari ini — tanpa catatan ini petani
 * menerima notifikasi yang sama sampai empat kali sehari untuk satu kejadian.
 *
 * Ini catatan operasional, bukan preferensi pengguna, karena itu ia tidak
 * menumpang di `user_preference`.
 *
 * [date] disimpan sebagai teks ISO, mengikuti `weather_cache`: project ini
 * sengaja tidak punya TypeConverter, dan bentuk ISO tetap bisa dibandingkan
 * langsung sebagai string.
 */
@Entity(tableName = "sent_alert")
data class SentAlertEntity(
    @PrimaryKey val id: Int = Constants.SENT_ALERT_ID,
    val regionCode: String,
    val date: String,
    val reason: String,
    /** Kapan notifikasinya ditampilkan; untuk diagnosa, tidak dipakai logika. */
    val notifiedAt: Long,
)
