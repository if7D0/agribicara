package com.agribicara.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agribicara.app.data.local.dao.SentAlertDao
import com.agribicara.app.data.local.entity.SentAlertEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Implementasi Room dari [SentAlertDao], bukan mock-nya.
 *
 * Ada karena perbaikan H3 bergantung sepenuhnya pada satu hal: catatan
 * peringatan benar-benar bisa ditulis lalu dibaca kembali. Test worker memakai
 * mockk, jadi kelas `SentAlertDao_Impl` bangkitan Room tidak pernah tersentuh
 * di sana — dan kalau round-trip-nya diam-diam gagal, dedup-nya lumpuh
 * sementara seluruh test lain tetap hijau. Itu pola kegagalan yang persis
 * meloloskan H1.
 *
 * Satu hal yang khusus diuji: [SentAlertDao.get] punya nilai default pada
 * parameternya. Default argument pada fungsi `suspend` abstrak di interface
 * dijembatani Kotlin lewat sintetis `DefaultImpls`, dan itu jalur yang layak
 * dibuktikan sekali, bukan diasumsikan.
 */
@RunWith(AndroidJUnit4::class)
class SentAlertDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: SentAlertDao

    @Before
    fun bukaDatabase() {
        // inMemory: test ini soal perilaku DAO, bukan soal berkas di disk.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.sentAlertDao()
    }

    @After
    fun tutupDatabase() {
        database.close()
    }

    @Test
    fun tanpaCatatanApaPunHasilnyaNull() = runTest {
        // Pemasangan baru: belum ada peringatan yang pernah dikirim, jadi
        // peringatan pertama harus boleh lewat.
        assertNull(dao.get())
    }

    @Test
    fun peringatanBisaDitulisLaluDibacaKembali() = runTest {
        dao.upsert(
            SentAlertEntity(
                regionCode = "11.01.01.2001",
                date = "2026-08-31",
                reason = "THUNDERSTORM",
                notifiedAt = 1700000000000L,
            ),
        )

        val tersimpan = dao.get()

        assertNotNull("Catatan peringatan tidak terbaca kembali", tersimpan)
        assertEquals("11.01.01.2001", tersimpan?.regionCode)
        assertEquals("2026-08-31", tersimpan?.date)
        assertEquals("THUNDERSTORM", tersimpan?.reason)
        assertEquals(1700000000000L, tersimpan?.notifiedAt)
    }

    @Test
    fun peringatanBaruMenimpaYangLamaBukanMenumpuk() = runTest {
        // Barisnya harus tetap satu. Kalau menumpuk, `get()` bisa mengembalikan
        // peringatan lama dan membungkam peringatan yang benar-benar baru.
        dao.upsert(
            SentAlertEntity(
                regionCode = "11.01.01.2001",
                date = "2026-08-31",
                reason = "THUNDERSTORM",
                notifiedAt = 1L,
            ),
        )
        dao.upsert(
            SentAlertEntity(
                regionCode = "11.01.01.2001",
                date = "2026-09-01",
                reason = "HEAVY_RAIN",
                notifiedAt = 2L,
            ),
        )

        val tersimpan = dao.get()

        assertEquals("2026-09-01", tersimpan?.date)
        assertEquals("HEAVY_RAIN", tersimpan?.reason)
    }
}
