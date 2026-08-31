package com.agribicara.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.agribicara.app.data.local.dao.ChatMessageDao
import com.agribicara.app.data.local.dao.RegionCacheDao
import com.agribicara.app.data.local.dao.SentAlertDao
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.dao.WeatherCacheDao
import com.agribicara.app.data.local.entity.ChatMessageEntity
import com.agribicara.app.data.local.entity.RegionCacheEntity
import com.agribicara.app.data.local.entity.SentAlertEntity
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import com.agribicara.app.data.local.entity.WeatherCacheEntity

/**
 * Database lokal AgriBicara.
 *
 * v1 (Fase 1): preferensi pengguna.
 * v2 (Fase 2): cache cuaca + cache wilayah.
 * v3 (Fase 5): riwayat percakapan.
 * v4 (Fase 6): peringatan cuaca terakhir yang sudah ditampilkan.
 *
 * Riwayat deteksi penyakit (Fase 4) menyusul —
 * setiap penambahan menaikkan [version] dan WAJIB disertai migrasi eksplisit
 * di `data/local/migration/Migrations.kt` plus test migrasinya. Schema
 * di-export ke app/schemas dan di-commit.
 */
@Database(
    entities = [
        UserPreferenceEntity::class,
        WeatherCacheEntity::class,
        RegionCacheEntity::class,
        ChatMessageEntity::class,
        SentAlertEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun weatherCacheDao(): WeatherCacheDao
    abstract fun regionCacheDao(): RegionCacheDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun sentAlertDao(): SentAlertDao
}
