package com.agribicara.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agribicara.app.core.common.Constants

/**
 * Preferensi pengguna — tabel baris tunggal (id selalu [Constants.USER_PREFERENCE_ID]).
 *
 * regionCode/regionName diisi oleh region picker di Fase 2.
 *
 * [latitude]/[longitude] TIDAK berasal dari picker: wilayah.id hanya
 * menyediakan kode dan nama. Koordinat baru diketahui dari response BMKG yang
 * pertama berhasil, lalu disimpan di sini supaya Open-Meteo tetap bisa
 * dipanggil di kemudian hari saat BMKG sedang mati. Konsekuensinya, bila BMKG
 * gagal pada pengambilan PERTAMA untuk sebuah wilayah, fallback Open-Meteo
 * belum bisa jalan dan app jatuh ke cache/pesan error — itu perilaku yang
 * disengaja dan ditangani eksplisit di WeatherRepositoryImpl.
 */
@Entity(tableName = "user_preference")
data class UserPreferenceEntity(
    @PrimaryKey val id: Int = Constants.USER_PREFERENCE_ID,
    val isOnboardingCompleted: Boolean = false,
    val regionCode: String? = null,
    val regionName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
