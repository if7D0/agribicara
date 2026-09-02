package com.agribicara.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agribicara.app.data.local.dao.ChatMessageDao
import com.agribicara.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Riwayat percakapan (Fase 5), yang sampai Fase 7 tidak punya test sama sekali.
 *
 * Yang paling penting di sini bukan tulis-baca biasa, melainkan `findAnswer`.
 * Query itulah yang memutuskan apakah sebuah jawaban lama boleh dipakai ulang
 * ketika Gemini gagal — dan memakai ulang jawaban yang salah lebih berbahaya
 * daripada tidak menjawab, karena petani tidak punya cara membedakannya dari
 * jawaban baru.
 */
@RunWith(AndroidJUnit4::class)
class ChatMessageDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ChatMessageDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.chatMessageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun pesan(
        role: String,
        text: String,
        questionKey: String = "kapan memupuk padi",
        regionCode: String? = "11.01.01.2001",
        createdAt: Long = 1_700_000_000_000,
    ) = ChatMessageEntity(
        role = role,
        text = text,
        questionKey = questionKey,
        regionCode = regionCode,
        createdAt = createdAt,
    )

    @Test
    fun pesanTersimpanDanTerbacaKembali() = runTest {
        dao.insert(pesan("USER", "kapan memupuk padi", createdAt = 1))
        dao.insert(pesan("ASSISTANT", "Pupuk sebaiknya Sabtu pagi.", createdAt = 2))

        val riwayat = dao.observeRecent(10).first()

        assertEquals(2, riwayat.size)
    }

    @Test
    fun pertanyaanYangSamaMenjadiDuaBarisBukanSalingMenimpa() = runTest {
        val idPertama = dao.insert(pesan("USER", "kapan memupuk padi", createdAt = 1))
        val idKedua = dao.insert(pesan("USER", "kapan memupuk padi", createdAt = 2))

        assertNotEquals(idPertama, idKedua)
        assertEquals(2, dao.observeRecent(10).first().size)
    }

    @Test
    fun riwayatDiurutkanTerbaruLebihDulu() = runTest {
        dao.insert(pesan("USER", "lama", createdAt = 1))
        dao.insert(pesan("USER", "baru", createdAt = 99))

        val riwayat = dao.observeRecent(10).first()

        assertEquals("baru", riwayat.first().text)
    }

    @Test
    fun limitDihormati() = runTest {
        repeat(5) { dao.insert(pesan("USER", "pesan $it", createdAt = it.toLong())) }

        assertEquals(2, dao.observeRecent(2).first().size)
    }

    // --- findAnswer: jalur yang menentukan jawaban dipakai ulang atau tidak --

    @Test
    fun jawabanTersimpanDitemukanUntukPertanyaanSetara() = runTest {
        dao.insert(pesan("ASSISTANT", "Pupuk sebaiknya Sabtu pagi.", createdAt = 100))

        val jawaban = dao.findAnswer("kapan memupuk padi", "11.01.01.2001", notBefore = 0)

        assertEquals("Pupuk sebaiknya Sabtu pagi.", jawaban)
    }

    @Test
    fun pesanPenggunaTidakPernahDikembalikanSebagaiJawaban() = runTest {
        // Tanpa saringan role, pertanyaan petani sendiri akan dibacakan
        // kembali kepadanya sebagai jawaban.
        dao.insert(pesan("USER", "kapan memupuk padi", createdAt = 100))

        assertNull(dao.findAnswer("kapan memupuk padi", "11.01.01.2001", notBefore = 0))
    }

    @Test
    fun jawabanWilayahLainTidakDipakaiUlang() = runTest {
        // Inti peringatan KDoc: jawaban yang di-grounding cuaca desa lain akan
        // menyesatkan. Cuaca Aceh Selatan bukan cuaca Cimahi.
        dao.insert(
            pesan("ASSISTANT", "Jawaban desa lain", regionCode = "32.77.01.1002", createdAt = 100),
        )

        assertNull(dao.findAnswer("kapan memupuk padi", "11.01.01.2001", notBefore = 0))
    }

    @Test
    fun jawabanTanpaWilayahHanyaCocokDenganPertanyaanTanpaWilayah() = runTest {
        // Kasus dua-duanya null ditangani eksplisit di SQL; tanpa itu
        // perbandingan NULL = NULL di SQLite selalu false dan jawaban tanpa
        // wilayah tidak akan pernah ditemukan.
        dao.insert(pesan("ASSISTANT", "Jawaban umum", regionCode = null, createdAt = 100))

        assertEquals(
            "Jawaban umum",
            dao.findAnswer("kapan memupuk padi", null, notBefore = 0),
        )
        assertNull(dao.findAnswer("kapan memupuk padi", "11.01.01.2001", notBefore = 0))
    }

    @Test
    fun jawabanBasiDisaringOlehNotBefore() = runTest {
        // Cuaca berubah, jadi jawaban lama bisa berbahaya, bukan sekadar usang.
        dao.insert(pesan("ASSISTANT", "Jawaban kemarin", createdAt = 100))

        assertNull(dao.findAnswer("kapan memupuk padi", "11.01.01.2001", notBefore = 101))
        assertEquals(
            "Jawaban kemarin",
            dao.findAnswer("kapan memupuk padi", "11.01.01.2001", notBefore = 100),
        )
    }

    @Test
    fun jawabanTerbaruYangDipakaiBukanYangPertama() = runTest {
        dao.insert(pesan("ASSISTANT", "Jawaban lama", createdAt = 100))
        dao.insert(pesan("ASSISTANT", "Jawaban baru", createdAt = 200))

        assertEquals(
            "Jawaban baru",
            dao.findAnswer("kapan memupuk padi", "11.01.01.2001", notBefore = 0),
        )
    }

    @Test
    fun trimMenyisakanYangTerbaruSaja() = runTest {
        repeat(6) { dao.insert(pesan("USER", "pesan $it", createdAt = it.toLong())) }

        dao.trimTo(3)

        val tersisa = dao.observeRecent(10).first()
        assertEquals(3, tersisa.size)
        assertEquals("pesan 5", tersisa.first().text)
    }
}
