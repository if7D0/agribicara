package com.agribicara.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.entity.UserPreferenceEntity

/**
 * Database lokal AgriBicara.
 *
 * Fase 1 hanya berisi preferensi pengguna. Tabel cuaca (Fase 2), riwayat chat
 * (Fase 5), dan riwayat deteksi penyakit (Fase 4) menyusul — setiap penambahan
 * menaikkan [version] dan membutuhkan migrasi (schema di-export ke app/schemas).
 */
@Database(
    entities = [UserPreferenceEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPreferenceDao(): UserPreferenceDao
}
