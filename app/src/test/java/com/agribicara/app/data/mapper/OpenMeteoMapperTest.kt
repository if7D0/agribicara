package com.agribicara.app.data.mapper

import com.agribicara.app.data.remote.openmeteo.dto.OpenMeteoDailyDto
import com.agribicara.app.data.remote.openmeteo.dto.OpenMeteoResponse
import com.agribicara.app.domain.model.WeatherSource
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoMapperTest {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private fun loadSample(): OpenMeteoResponse {
        val raw = requireNotNull(javaClass.classLoader?.getResourceAsStream("openmeteo_sample.json"))
            .bufferedReader().use { it.readText() }
        return json.decodeFromString(OpenMeteoResponse.serializer(), raw)
    }

    @Test
    fun `response nyata menghasilkan tujuh hari`() {
        val days = OpenMeteoMapper.toDailyForecasts(loadSample())
        assertEquals(7, days.size)
    }

    @Test
    fun `semua hari ditandai bersumber Open-Meteo`() {
        val days = OpenMeteoMapper.toDailyForecasts(loadSample())
        assertTrue(days.all { it.source == WeatherSource.OPEN_METEO })
    }

    @Test
    fun `deskripsi selalu null karena Open-Meteo hanya mengirim kode`() {
        // UI wajib menerjemahkan lewat WeatherCodeMapper untuk sumber ini.
        val days = OpenMeteoMapper.toDailyForecasts(loadSample())
        assertTrue(days.all { it.description == null })
    }

    @Test
    fun `array paralel dipasangkan lewat indeks yang sama`() {
        val response = OpenMeteoResponse(
            daily = OpenMeteoDailyDto(
                time = listOf("2026-08-29", "2026-08-30"),
                temperatureMax = listOf(30.9, 30.7),
                temperatureMin = listOf(17.8, 18.0),
                precipitationSum = listOf(0.0, 0.1),
                windSpeedMax = listOf(13.5, 14.4),
                weatherCode = listOf(2, 51),
            ),
        )
        val days = OpenMeteoMapper.toDailyForecasts(response)
        assertEquals(LocalDate.of(2026, 8, 30), days[1].date)
        assertEquals(30.7, days[1].temperatureMax!!, 0.001)
        assertEquals(51, days[1].weatherCode)
    }

    @Test
    fun `array yang panjangnya tidak sama dipotong ke yang terpendek`() {
        // Response cacat sebagian tidak boleh menyebabkan IndexOutOfBounds.
        val response = OpenMeteoResponse(
            daily = OpenMeteoDailyDto(
                time = listOf("2026-08-29", "2026-08-30", "2026-08-31"),
                temperatureMax = listOf(30.9, 30.7),
                temperatureMin = listOf(17.8, 18.0),
                precipitationSum = listOf(0.0, 0.1),
                windSpeedMax = listOf(13.5, 14.4),
                weatherCode = listOf(2, 51),
            ),
        )
        assertEquals(2, OpenMeteoMapper.toDailyForecasts(response).size)
    }

    @Test
    fun `field opsional yang tidak dikirim tidak memangkas prakiraan jadi nol`() {
        // windSpeedMax kosong sama sekali; hari tetap harus terbentuk.
        val response = OpenMeteoResponse(
            daily = OpenMeteoDailyDto(
                time = listOf("2026-08-29", "2026-08-30"),
                temperatureMax = listOf(30.9, 30.7),
                temperatureMin = listOf(17.8, 18.0),
                precipitationSum = listOf(0.0, 0.1),
                windSpeedMax = emptyList(),
                weatherCode = listOf(2, 51),
            ),
        )
        val days = OpenMeteoMapper.toDailyForecasts(response)
        assertEquals(2, days.size)
        assertNull(days[0].windSpeed)
    }

    @Test
    fun `daily null menghasilkan daftar kosong`() {
        assertEquals(emptyList<Any>(), OpenMeteoMapper.toDailyForecasts(OpenMeteoResponse()))
    }

    @Test
    fun `tanggal rusak dibuang tanpa menjatuhkan sisanya`() {
        val response = OpenMeteoResponse(
            daily = OpenMeteoDailyDto(
                time = listOf("bukan-tanggal", "2026-08-30"),
                temperatureMax = listOf(30.9, 30.7),
                temperatureMin = listOf(17.8, 18.0),
                precipitationSum = listOf(0.0, 0.1),
                windSpeedMax = listOf(13.5, 14.4),
                weatherCode = listOf(2, 51),
            ),
        )
        val days = OpenMeteoMapper.toDailyForecasts(response)
        assertEquals(1, days.size)
        assertEquals(LocalDate.of(2026, 8, 30), days[0].date)
    }
}
