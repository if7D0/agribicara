package com.agribicara.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: menambah cache cuaca dan cache wilayah (Fase 2).
 *
 * Dua tabel baru, plus dua kolom koordinat pada `user_preference`. Tidak ada
 * data lama yang dihapus atau ditulis ulang: ADD COLUMN pada SQLite hanya
 * menambah kolom bernilai NULL, sehingga flag onboarding dan pilihan wilayah
 * pengguna selamat.
 *
 * Migrasi ditulis eksplisit, BUKAN fallbackToDestructiveMigration(): opsi itu
 * akan menghapus seluruh database pengguna pada setiap kenaikan versi.
 *
 * SQL di bawah harus persis sama dengan yang dibangkitkan Room untuk kedua
 * entity. Bila tidak cocok, MigrationTest akan gagal dengan diff skema — itu
 * memang gunanya test tersebut.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Koordinat kelurahan, dipelajari dari response BMKG pertama yang
        // berhasil. Dibutuhkan agar Open-Meteo tetap bisa dipanggil ketika
        // BMKG sedang mati.
        db.execSQL("ALTER TABLE `user_preference` ADD COLUMN `latitude` REAL")
        db.execSQL("ALTER TABLE `user_preference` ADD COLUMN `longitude` REAL")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `weather_cache` (
                `regionCode` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `temperatureMax` REAL,
                `temperatureMin` REAL,
                `precipitationMm` REAL,
                `windSpeed` REAL,
                `weatherCode` INTEGER,
                `description` TEXT,
                `source` TEXT NOT NULL,
                `fetchedAt` INTEGER NOT NULL,
                PRIMARY KEY(`regionCode`, `date`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_weather_cache_regionCode` " +
                "ON `weather_cache` (`regionCode`)",
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `region_cache` (
                `code` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `level` TEXT NOT NULL,
                `parentCode` TEXT,
                PRIMARY KEY(`code`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_region_cache_parentCode` " +
                "ON `region_cache` (`parentCode`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_region_cache_level` " +
                "ON `region_cache` (`level`)",
        )
    }
}

/**
 * v2 -> v3: riwayat percakapan (Fase 5).
 *
 * Hanya menambah satu tabel baru. Tidak ada tabel lama yang disentuh, jadi
 * cuaca tersimpan, wilayah tersimpan, dan preferensi pengguna semuanya
 * selamat — penting karena cache cuaca adalah satu-satunya sumber data saat
 * petani sedang offline.
 *
 * SQL di bawah disalin PERSIS dari app/schemas/.../3.json (`createSql`),
 * bukan ditulis dari ingatan. MigrationTest membandingkan hasil migrasi ini
 * dengan skema bangkitan Room; perbedaan sekecil apa pun, termasuk
 * AUTOINCREMENT yang terlewat, akan menggagalkannya.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `chat_message` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`role` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`questionKey` TEXT NOT NULL, " +
                "`regionCode` TEXT, " +
                "`createdAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_chat_message_questionKey` " +
                "ON `chat_message` (`questionKey`)",
        )
    }
}
