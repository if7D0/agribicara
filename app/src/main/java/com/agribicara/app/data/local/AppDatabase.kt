package com.agribicara.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agribicara.app.data.local.dao.RegionCacheDao
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.dao.WeatherCacheDao
import com.agribicara.app.data.local.entity.RegionCacheEntity
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import com.agribicara.app.data.local.entity.WeatherCacheEntity

/**
 * Database lokal AgriBicara.
 *
 * v1 (Fase 1): preferensi pengguna.
 * v2 (Fase 2): cache cuaca + cache wilayah.
 *
 * Riwayat chat (Fase 5) dan riwayat deteksi penyakit (Fase 4) menyusul —
 * setiap penambahan menaikkan [version] dan WAJIB disertai migrasi eksplisit
 * di `data/local/migration/Migrations.kt` plus test migrasinya. Schema
 * di-export ke app/schemas dan di-commit.
 */
@Database(
    entities = [
        UserPreferenceEntity::class,
        WeatherCacheEntity::class,
        RegionCacheEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun regionCacheDao(): RegionCacheDao
}
