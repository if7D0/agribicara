package com.agribicara.app.data.mapper

import com.agribicara.app.data.remote.bmkg.dto.BmkgForecastResponse
import com.agribicara.app.domain.model.WeatherSource
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Diuji terhadap response BMKG SUNGGUHAN (app/src/test/resources/bmkg_sample.json,
 * diambil 2026-08-29), bukan JSON karangan. Bentuk aslinya justru yang paling
 * mungkin mengandung kejutan.
 */
class BmkgMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun loadSample(): BmkgForecastResponse {
        val raw = requireNotNull(javaClass.classLoader?.getResourceAsStream("bmkg_sample.json"))
            .bufferedReader().use { it.readText() }
        return json.decodeFromString(BmkgForecastResponse.serializer(), raw)
    }

    @Test
    fun `response nyata menghasilkan tiga hari, bukan tujuh`() {
        // Inti temuan spike: BMKG TIDAK menyediakan 7 hari.
        val days = BmkgMapper.toDailyForecasts(loadSample())
        assertEquals(3, days.size)
    }

    @Test
    fun `hari pertama parsial tidak dibuang dan tidak dianggap rusak`() {
        // Grup pertama hanya berisi 3 entri (mulai pukul 17:00), bukan 8.
        // Pengelompokan berbasis indeks akan salah di sini.
        val days = BmkgMapper.toDailyForecasts(loadSample())
        assertEquals(LocalDate.of(2026, 8, 29), days[0].date)
        assertEquals(3, days[0].hourly.size)
        assertEquals(8, days[1].hourly.size)
        assertEquals(8, days[2].hourly.size)
    }

    @Test
    fun `hari diurutkan menaik dan berurutan`() {
        val days = BmkgMapper.toDailyForecasts(loadSample())
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 29),
                LocalDate.of(2026, 8, 30),
                LocalDate.of(2026, 8, 31),
            ),
            days.map { it.date },
        )
    }

    @Test
    fun `pengelompokan memakai tanggal lokal bukan UTC`() {
        // Entri 2026-08-29 23:00 WIB adalah 16:00 UTC di hari yang sama,
        // tapi 2026-08-30 02:00 WIB adalah 19:00 UTC pada 29 Agustus.
        // Memakai UTC akan menaruh entri dini hari di hari yang salah.
        val days = BmkgMapper.toDailyForecasts(loadSample())
        val secondDay = days[1]
        assertTrue(secondDay.hourly.all { it.time.toLocalDate() == LocalDate.of(2026, 8, 30) })
        assertEquals(2, secondDay.hourly.first().time.hour)
    }

    @Test
    fun `semua hari ditandai bersumber BMKG`() {
        val days = BmkgMapper.toDailyForecasts(loadSample())
        assertTrue(days.all { it.source == WeatherSource.BMKG })
    }

    @Test
    fun `ringkasan harian memakai suhu maksimum dan minimum dari entri hari itu`() {
        val days = BmkgMapper.toDailyForecasts(loadSample())
        val firstDay = days[0]
        val temps = firstDay.hourly.mapNotNull { it.temperatureCelsius }
        assertEquals(temps.max(), firstDay.temperatureMax!!, 0.001)
        assertEquals(temps.min(), firstDay.temperatureMin!!, 0.001)
    }

    @Test
    fun `deskripsi Bahasa Indonesia dari BMKG dipertahankan`() {
        val days = BmkgMapper.toDailyForecasts(loadSample())
        // BMKG mengirim weather_desc siap tampil; kita tidak boleh membuangnya
        // dan menerjemahkan ulang dari kode.
        assertNotNull(days[0].description)
        assertTrue(days.all { it.description == null || it.description!!.isNotBlank() })
    }

    @Test
    fun `response kosong menghasilkan daftar kosong tanpa exception`() {
        val empty = json.decodeFromString(BmkgForecastResponse.serializer(), """{"data":[]}""")
        assertEquals(emptyList<Any>(), BmkgMapper.toDailyForecasts(empty))
    }

    @Test
    fun `array cuaca kosong di dalam data juga aman`() {
        val empty = json.decodeFromString(
            BmkgForecastResponse.serializer(),
            """{"data":[{"cuaca":[[]]}]}""",
        )
        assertEquals(emptyList<Any>(), BmkgMapper.toDailyForecasts(empty))
    }

    @Test
    fun `entri dengan waktu rusak dibuang tanpa menjatuhkan sisanya`() {
        val mixed = json.decodeFromString(
            BmkgForecastResponse.serializer(),
            """
            {"data":[{"cuaca":[[
              {"local_datetime":"bukan-tanggal","t":25,"weather":1},
              {"local_datetime":"2026-08-29 17:00:00","t":26,"weather":1}
            ]]}]}
            """.trimIndent(),
        )
        val days = BmkgMapper.toDailyForecasts(mixed)
        assertEquals(1, days.size)
        assertEquals(1, days[0].hourly.size)
    }

    @Test
    fun `nama wilayah dirangkai dari desa dan kecamatan`() {
        assertEquals("Cibeureum, Cimahi Selatan", BmkgMapper.regionDisplayName(loadSample()))
    }

    @Test
    fun `nama wilayah null bila lokasi tidak dikirim`() {
        val empty = json.decodeFromString(BmkgForecastResponse.serializer(), """{"data":[]}""")
        assertNull(BmkgMapper.regionDisplayName(empty))
    }
}
