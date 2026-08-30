package com.agribicara.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.agribicara.app.data.local.dao.RegionCacheDao
import com.agribicara.app.data.local.dao.UserPreferenceDao
import com.agribicara.app.data.local.dao.WeatherCacheDao
import com.agribicara.app.data.local.entity.RegionCacheEntity
import com.agribicara.app.data.local.entity.UserPreferenceEntity
import com.agribicara.app.data.local.entity.WeatherCacheEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherCacheDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var weatherDao: WeatherCacheDao
    private lateinit var regionDao: RegionCacheDao
    private lateinit var preferenceDao: UserPreferenceDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        weatherDao = database.weatherCacheDao()
        regionDao = database.regionCacheDao()
        preferenceDao = database.userPreferenceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun entity(date: String, regionCode: String = "32.77.01.1002") = WeatherCacheEntity(
        regionCode = regionCode,
        date = date,
        temperatureMax = 30.0,
        temperatureMin = 20.0,
        precipitationMm = 0.0,
        windSpeed = 3.6,
        weatherCode = 1,
        description = "Cerah",
        source = "BMKG",
        fetchedAt = 1_700_000_000_000,
    )

    @Test
    fun tulisDanBacaKembali() = runTest {
        weatherDao.upsertAll(listOf(entity("2026-08-29"), entity("2026-08-30")))

        val rows = weatherDao.getForRegion("32.77.01.1002")

        assertEquals(2, rows.size)
        assertEquals("2026-08-29", rows[0].date)
    }

    @Test
    fun primaryKeyGabunganMenimpaBukanMenumpuk() = runTest {
        // Refresh berulang untuk hari yang sama tidak boleh menggelembungkan tabel.
        weatherDao.upsertAll(listOf(entity("2026-08-29")))
        weatherDao.upsertAll(listOf(entity("2026-08-29").copy(temperatureMax = 35.0)))

        val rows = weatherDao.getForRegion("32.77.01.1002")

        assertEquals(1, rows.size)
        assertEquals(35.0, rows[0].temperatureMax!!, 0.001)
    }

    @Test
    fun replaceForRegionMembuangHariYangSudahTidakDikirimLagi() = runTest {
        weatherDao.upsertAll(listOf(entity("2026-08-28"), entity("2026-08-29")))

        weatherDao.replaceForRegion("32.77.01.1002", listOf(entity("2026-08-29")))

        val rows = weatherDao.getForRegion("32.77.01.1002")
        assertEquals(1, rows.size)
        assertEquals("2026-08-29", rows[0].date)
    }

    @Test
    fun wilayahLainTidakIkutTerhapus() = runTest {
        weatherDao.upsertAll(listOf(entity("2026-08-29"), entity("2026-08-29", "11.01.01.2001")))

        weatherDao.replaceForRegion("32.77.01.1002", emptyList())

        assertTrue(weatherDao.getForRegion("32.77.01.1002").isEmpty())
        assertEquals(1, weatherDao.getForRegion("11.01.01.2001").size)
    }

    @Test
    fun hasilTerurutMenaikBerdasarkanTanggal() = runTest {
        weatherDao.upsertAll(
            listOf(entity("2026-08-31"), entity("2026-08-29"), entity("2026-08-30")),
        )

        val dates = weatherDao.getForRegion("32.77.01.1002").map { it.date }

        assertEquals(listOf("2026-08-29", "2026-08-30", "2026-08-31"), dates)
    }

    @Test
    fun cacheWilayahDifilterBerdasarkanIndukTermasukNull() = runTest {
        regionDao.upsertAll(
            listOf(
                RegionCacheEntity("32", "Jawa Barat", "PROVINCE", null),
                RegionCacheEntity("33", "Jawa Tengah", "PROVINCE", null),
                RegionCacheEntity("32.77", "Kota Cimahi", "REGENCY", "32"),
            ),
        )

        // parentCode NULL memerlukan "IS", bukan "=" — kesalahan klasik SQL
        // yang membuat daftar provinsi selalu kosong.
        assertEquals(2, regionDao.getByLevel("PROVINCE", null).size)
        assertEquals(1, regionDao.getByLevel("REGENCY", "32").size)
        assertTrue(regionDao.getByLevel("REGENCY", "99").isEmpty())
    }

    @Test
    fun updateRegionBersifatAtomikDanTidakMenghapusFlagOnboarding() = runTest {
        preferenceDao.markOnboardingCompleted()

        preferenceDao.updateRegion(code = "32.77.01.1002", name = "Cibeureum")

        val preference = preferenceDao.get(1)
        assertEquals("32.77.01.1002", preference?.regionCode)
        assertEquals("Cibeureum", preference?.regionName)
        // Inilah yang dilindungi oleh UPDATE bertarget: read-modify-write bisa
        // mengembalikan flag ini ke false.
        assertEquals(true, preference?.isOnboardingCompleted)
    }

    @Test
    fun updateRegionMembuatBarisBilaBelumAda() = runTest {
        preferenceDao.updateRegion(code = "32.77.01.1002", name = "Cibeureum")

        assertEquals("Cibeureum", preferenceDao.get(1)?.regionName)
    }

    @Test
    fun updateCoordinatesHanyaMenyentuhKolomKoordinat() = runTest {
        preferenceDao.upsert(
            UserPreferenceEntity(
                id = 1,
                isOnboardingCompleted = true,
                regionCode = "32.77.01.1002",
                regionName = "Cibeureum",
            ),
        )

        preferenceDao.updateCoordinates(1, -6.9079, 107.5590)

        val preference = preferenceDao.get(1)
        assertEquals(-6.9079, preference?.latitude!!, 0.0001)
        assertEquals("Cibeureum", preference.regionName)
        assertEquals(true, preference.isOnboardingCompleted)
    }

    @Test
    fun koordinatNullSebelumBmkgPernahBerhasil() = runTest {
        preferenceDao.updateRegion(code = "32.77.01.1002", name = "Cibeureum")

        val preference = preferenceDao.get(1)
        assertNull(preference?.latitude)
        assertNull(preference?.longitude)
    }

    @Test
    fun gantiWilayahMenghapusKoordinatWilayahLama() = runTest {
        // Tanpa ini, saat BMKG gagal untuk wilayah baru, fallback Open-Meteo
        // akan mengambil cuaca lokasi LAMA dan menampilkannya dengan nama
        // wilayah BARU — salah data yang tidak terlihat oleh pengguna.
        preferenceDao.updateRegion(code = "32.77.01.1002", name = "Cibeureum")
        preferenceDao.updateCoordinates(1, -6.9079, 107.5590)
        assertEquals(-6.9079, preferenceDao.get(1)?.latitude!!, 0.0001)

        preferenceDao.updateRegion(code = "33.74.01.1001", name = "Semarang Barat")

        val preference = preferenceDao.get(1)
        assertEquals("33.74.01.1001", preference?.regionCode)
        assertNull("Koordinat wilayah lama harus ikut terhapus", preference?.latitude)
        assertNull("Koordinat wilayah lama harus ikut terhapus", preference?.longitude)
    }

    @Test
    fun gantiWilayahTidakMengubahFlagOnboarding() = runTest {
        preferenceDao.markOnboardingCompleted()
        preferenceDao.updateRegion(code = "32.77.01.1002", name = "Cibeureum")

        preferenceDao.updateRegion(code = "33.74.01.1001", name = "Semarang Barat")

        assertEquals(true, preferenceDao.get(1)?.isOnboardingCompleted)
    }
}
