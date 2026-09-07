package com.agribicara.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agribicara.app.data.local.dao.DetectionDao
import com.agribicara.app.data.local.entity.DetectionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Riwayat deteksi penyakit (Fase 4).
 *
 * Pola sama dengan [ChatMessageDaoTest]: id auto-generate, urutan terbaru dulu,
 * limit dihormati, dan trim menyisakan yang terbaru.
 */
@RunWith(AndroidJUnit4::class)
class DetectionDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DetectionDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.detectionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun deteksi(
        outcomeType: String = "DIAGNOSED",
        label: String? = "blast",
        confidence: Float = 0.9f,
        createdAt: Long = 1_700_000_000_000,
    ) = DetectionEntity(
        outcomeType = outcomeType,
        label = label,
        confidence = confidence,
        createdAt = createdAt,
    )

    @Test
    fun deteksiTersimpanDanTerbacaKembali() = runTest {
        dao.insert(deteksi(createdAt = 1))
        dao.insert(deteksi(outcomeType = "UNSURE", label = null, confidence = 0.3f, createdAt = 2))

        assertEquals(2, dao.observeRecent(10).first().size)
    }

    @Test
    fun deteksiSerupaMenjadiDuaBarisBukanSalingMenimpa() = runTest {
        val id1 = dao.insert(deteksi(createdAt = 1))
        val id2 = dao.insert(deteksi(createdAt = 2))

        assertNotEquals(id1, id2)
        assertEquals(2, dao.observeRecent(10).first().size)
    }

    @Test
    fun riwayatDiurutkanTerbaruLebihDulu() = runTest {
        dao.insert(deteksi(label = "brown_spot", createdAt = 1))
        dao.insert(deteksi(label = "blast", createdAt = 99))

        assertEquals("blast", dao.observeRecent(10).first().first().label)
    }

    @Test
    fun limitDihormati() = runTest {
        repeat(5) { dao.insert(deteksi(createdAt = it.toLong())) }

        assertEquals(2, dao.observeRecent(2).first().size)
    }

    @Test
    fun trimMenyisakanYangTerbaruSaja() = runTest {
        repeat(6) { dao.insert(deteksi(label = "p$it", createdAt = it.toLong())) }

        dao.trimTo(3)

        val tersisa = dao.observeRecent(10).first()
        assertEquals(3, tersisa.size)
        assertEquals("p5", tersisa.first().label)
    }

    @Test
    fun labelNullTersimpanUntukHasilUnsure() = runTest {
        dao.insert(deteksi(outcomeType = "UNSURE", label = null, confidence = 0.2f, createdAt = 1))

        assertEquals(null, dao.observeRecent(1).first().first().label)
    }
}
