package com.agribicara.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agribicara.app.data.local.dao.RegionCacheDao
import com.agribicara.app.data.local.entity.RegionCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cache wilayah bertingkat (Fase 2), yang sampai Fase 7 tidak punya test.
 *
 * Region picker adalah satu-satunya jalan petani memilih desanya, dan BMKG
 * hanya menerima kode `adm4` — tidak ada reverse-geocode, tidak ada GPS. Kalau
 * cache ini salah menyaring, petani tidak bisa menemukan desanya sama sekali.
 */
@RunWith(AndroidJUnit4::class)
class RegionCacheDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RegionCacheDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        dao = database.regionCacheDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun wilayah(
        code: String,
        name: String,
        level: String,
        parentCode: String?,
    ) = RegionCacheEntity(code = code, name = name, level = level, parentCode = parentCode)

    /** Aceh -> Aceh Selatan -> Bakongan -> Keude Bakongan, wilayah uji proyek ini. */
    private suspend fun isiEmpatTingkat() {
        dao.upsertAll(
            listOf(
                wilayah("11", "Aceh", "PROVINCE", null),
                wilayah("32", "Jawa Barat", "PROVINCE", null),
                wilayah("11.01", "Aceh Selatan", "REGENCY", "11"),
                wilayah("11.02", "Aceh Tenggara", "REGENCY", "11"),
                wilayah("32.77", "Cimahi", "REGENCY", "32"),
                wilayah("11.01.01", "Bakongan", "DISTRICT", "11.01"),
                wilayah("11.01.01.2001", "Keude Bakongan", "VILLAGE", "11.01.01"),
            ),
        )
    }

    @Test
    fun provinsiDitemukanLewatParentCodeNull() = runTest {
        // Provinsi tidak punya induk. SQL memakai `IS :parentCode`, bukan `=`,
        // karena di SQLite perbandingan NULL = NULL selalu false dan seluruh
        // daftar provinsi akan kosong.
        isiEmpatTingkat()

        val provinsi = dao.getByLevel("PROVINCE", null)

        assertEquals(2, provinsi.size)
    }

    @Test
    fun hanyaAnakLangsungYangDikembalikan() = runTest {
        isiEmpatTingkat()

        val kabupaten = dao.getByLevel("REGENCY", "11")

        assertEquals(2, kabupaten.size)
        assertTrue(kabupaten.all { it.parentCode == "11" })
    }

    @Test
    fun tingkatYangSalahTidakIkutTerbawa() = runTest {
        // Tanpa saringan level, "11.01.01" (kecamatan) bisa muncul di daftar
        // kabupaten hanya karena kode induknya cocok sebagian.
        isiEmpatTingkat()

        val desa = dao.getByLevel("VILLAGE", "11.01.01")

        assertEquals(1, desa.size)
        assertEquals("Keude Bakongan", desa.first().name)
    }

    @Test
    fun hasilDiurutkanBerdasarkanNama() = runTest {
        dao.upsertAll(
            listOf(
                wilayah("11.03", "Zulu", "REGENCY", "11"),
                wilayah("11.01", "Aceh Selatan", "REGENCY", "11"),
                wilayah("11.02", "Meulaboh", "REGENCY", "11"),
            ),
        )

        val hasil = dao.getByLevel("REGENCY", "11").map { it.name }

        // Urutan abjad, bukan urutan kode. Petani mencari nama desanya, bukan
        // kodenya.
        assertEquals(listOf("Aceh Selatan", "Meulaboh", "Zulu"), hasil)
    }

    @Test
    fun upsertUlangTidakMenggandakanBaris() = runTest {
        // Setiap refresh menulis ulang seluruh daftar. Kalau menggandakan,
        // daftar desa akan tumbuh setiap kali layar dibuka.
        val aceh = listOf(wilayah("11", "Aceh", "PROVINCE", null))
        dao.upsertAll(aceh)
        dao.upsertAll(aceh)

        assertEquals(1, dao.getByLevel("PROVINCE", null).size)
    }

    @Test
    fun upsertMemperbaruiNamaYangBerubah() = runTest {
        dao.upsertAll(listOf(wilayah("11", "Aceh", "PROVINCE", null)))
        dao.upsertAll(listOf(wilayah("11", "Aceh (baru)", "PROVINCE", null)))

        assertEquals("Aceh (baru)", dao.getByLevel("PROVINCE", null).single().name)
    }

    @Test
    fun kodeIndukTakDikenalMengembalikanDaftarKosongBukanCrash() = runTest {
        isiEmpatTingkat()

        assertTrue(dao.getByLevel("VILLAGE", "99.99.99").isEmpty())
    }
}
