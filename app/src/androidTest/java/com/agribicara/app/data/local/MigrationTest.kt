package com.agribicara.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.agribicara.app.data.local.migration.MIGRATION_1_2
import com.agribicara.app.data.local.migration.MIGRATION_2_3
import com.agribicara.app.data.local.migration.MIGRATION_3_4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Membuktikan migrasi v1 -> v2 TIDAK menghapus data pengguna.
 *
 * Ini test paling penting di Fase 2 dari sudut pandang pengguna: kegagalan di
 * sini berarti setiap pemasangan update menghapus pilihan wilayah dan
 * memaksa petani mengulang onboarding — kerusakan yang tidak terlihat di
 * instalasi baru dan hanya muncul pada pengguna yang sudah ada.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private companion object {
        const val TEST_DB = "migration-test.db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrasi1Ke2MempertahankanPreferensiPengguna() {
        // v1: tulis preferensi lengkap seperti pengguna Fase 1 yang sudah
        // menyelesaikan onboarding.
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO user_preference " +
                    "(id, isOnboardingCompleted, regionCode, regionName) " +
                    "VALUES (1, 1, '32.77.01.1002', 'Cibeureum')",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT * FROM user_preference WHERE id = 1").use { cursor ->
            assertTrue("Baris preferensi hilang setelah migrasi", cursor.moveToFirst())
            assertEquals(
                1,
                cursor.getInt(cursor.getColumnIndexOrThrow("isOnboardingCompleted")),
            )
            assertEquals(
                "32.77.01.1002",
                cursor.getString(cursor.getColumnIndexOrThrow("regionCode")),
            )
            assertEquals(
                "Cibeureum",
                cursor.getString(cursor.getColumnIndexOrThrow("regionName")),
            )
            // Kolom koordinat baru ada dan bernilai NULL sampai BMKG mengisinya.
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("latitude")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("longitude")))
        }
    }

    @Test
    fun migrasi1Ke2MembuatTabelBaruYangBisaDitulis() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.execSQL(
            "INSERT INTO weather_cache " +
                "(regionCode, date, temperatureMax, temperatureMin, precipitationMm, " +
                "windSpeed, weatherCode, description, source, fetchedAt) " +
                "VALUES ('32.77.01.1002', '2026-08-29', 30.0, 20.0, 0.0, 3.6, 1, " +
                "'Cerah', 'BMKG', 1700000000000)",
        )
        db.execSQL(
            "INSERT INTO region_cache (code, name, level, parentCode) " +
                "VALUES ('32', 'Jawa Barat', 'PROVINCE', NULL)",
        )

        db.query("SELECT COUNT(*) FROM weather_cache").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM region_cache").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun migrasi1Ke2AmanPadaDatabaseKosong() {
        // Pengguna yang membuka app tapi belum pernah menyelesaikan onboarding
        // punya tabel tanpa baris sama sekali.
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        db.query("SELECT COUNT(*) FROM user_preference").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrasi2Ke3MempertahankanCuacaTersimpan() {
        // Cache cuaca adalah satu-satunya sumber data saat petani offline.
        // Kehilangannya karena update adalah kerusakan yang nyata terasa.
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO user_preference " +
                    "(id, isOnboardingCompleted, regionCode, regionName, latitude, longitude) " +
                    "VALUES (1, 1, '32.77.01.1002', 'Cibeureum', -6.9, 107.5)",
            )
            db.execSQL(
                "INSERT INTO weather_cache " +
                    "(regionCode, date, temperatureMax, temperatureMin, precipitationMm, " +
                    "windSpeed, weatherCode, description, source, fetchedAt) " +
                    "VALUES ('32.77.01.1002', '2026-08-29', 30.0, 20.0, 0.0, 3.6, 1, " +
                    "'Cerah', 'BMKG', 1700000000000)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.query("SELECT COUNT(*) FROM weather_cache").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT regionName FROM user_preference WHERE id = 1").use { cursor ->
            assertTrue("Preferensi hilang setelah migrasi", cursor.moveToFirst())
            assertEquals("Cibeureum", cursor.getString(0))
        }
    }

    @Test
    fun migrasi2Ke3MembuatTabelRiwayatYangBisaDitulis() {
        helper.createDatabase(TEST_DB, 2).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.execSQL(
            "INSERT INTO chat_message (role, text, questionKey, regionCode, createdAt) " +
                "VALUES ('USER', 'kapan memupuk padi', 'kapan memupuk padi', " +
                "'32.77.01.1002', 1700000000000)",
        )
        db.execSQL(
            "INSERT INTO chat_message (role, text, questionKey, regionCode, createdAt) " +
                "VALUES ('ASSISTANT', 'Pupuk sebaiknya Sabtu pagi.', 'kapan memupuk padi', " +
                "'32.77.01.1002', 1700000000001)",
        )

        db.query("SELECT COUNT(*) FROM chat_message").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(2, cursor.getInt(0))
        }
        // id auto-generate: dua baris pertanyaan yang sama harus tetap
        // menjadi dua baris, bukan saling menimpa.
        db.query("SELECT DISTINCT id FROM chat_message").use { cursor ->
            assertEquals(2, cursor.count)
        }
    }

    @Test
    fun migrasi2Ke3AmanPadaDatabaseKosong() {
        helper.createDatabase(TEST_DB, 2).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        db.query("SELECT COUNT(*) FROM chat_message").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrasi3Ke4MempertahankanSeluruhDataLama() {
        // Peringatan cuaca (Fase 6) menambah satu tabel. Riwayat percakapan,
        // cache cuaca, dan preferensi harus lolos tanpa tergores.
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO user_preference " +
                    "(id, isOnboardingCompleted, regionCode, regionName, latitude, longitude) " +
                    "VALUES (1, 1, '11.01.01.2001', 'Keude Bakongan', 3.0, 97.4)",
            )
            db.execSQL(
                "INSERT INTO weather_cache " +
                    "(regionCode, date, temperatureMax, temperatureMin, precipitationMm, " +
                    "windSpeed, weatherCode, description, source, fetchedAt) " +
                    "VALUES ('11.01.01.2001', '2026-08-31', 31.0, 24.0, 0.0, 4.0, 1, " +
                    "'Cerah', 'BMKG', 1700000000000)",
            )
            db.execSQL(
                "INSERT INTO chat_message (role, text, questionKey, regionCode, createdAt) " +
                    "VALUES ('USER', 'kapan memupuk padi', 'kapan memupuk padi', " +
                    "'11.01.01.2001', 1700000000000)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        db.query("SELECT regionName FROM user_preference WHERE id = 1").use { cursor ->
            assertTrue("Preferensi hilang setelah migrasi", cursor.moveToFirst())
            assertEquals("Keude Bakongan", cursor.getString(0))
        }
        db.query("SELECT COUNT(*) FROM weather_cache").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM chat_message").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun migrasi3Ke4MembuatTabelPeringatanYangBisaDitulis() {
        helper.createDatabase(TEST_DB, 3).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        db.execSQL(
            "INSERT INTO sent_alert (id, regionCode, date, reason, notifiedAt) " +
                "VALUES (1, '11.01.01.2001', '2026-08-31', 'THUNDERSTORM', 1700000000000)",
        )
        // Baris tunggal: peringatan berikutnya MENIMPA, tidak menumpuk.
        db.execSQL(
            "INSERT OR REPLACE INTO sent_alert (id, regionCode, date, reason, notifiedAt) " +
                "VALUES (1, '11.01.01.2001', '2026-09-01', 'HEAVY_RAIN', 1700000003600)",
        )

        db.query("SELECT COUNT(*) FROM sent_alert").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT reason FROM sent_alert WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("HEAVY_RAIN", cursor.getString(0))
        }
    }

    @Test
    fun migrasi3Ke4MulaiDenganTabelKosong() {
        // Sengaja kosong: pengguna yang meng-update bisa menerima satu kali
        // peringatan ulang, dan itu lebih jujur daripada menebak isi baris
        // yang datanya memang belum pernah ada.
        helper.createDatabase(TEST_DB, 3).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        db.query("SELECT COUNT(*) FROM sent_alert").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }
}
