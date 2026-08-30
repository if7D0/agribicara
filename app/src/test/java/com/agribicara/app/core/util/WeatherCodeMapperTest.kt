package com.agribicara.app.core.util

import com.agribicara.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Kode WMO dipakai oleh BMKG DAN Open-Meteo, jadi mapper ini melayani
 * keduanya. Kode yang terdaftar di bawah diambil dari response BMKG nyata.
 */
class WeatherCodeMapperTest {

    @Test
    fun `kode 0 dan 1 sama-sama Cerah`() {
        // BMKG mengirim keduanya dengan weather_desc "Cerah" — melewatkan
        // salah satunya membuat sebagian hari tampil "Tidak diketahui".
        assertEquals(R.string.weather_clear, WeatherCodeMapper.labelFor(0))
        assertEquals(R.string.weather_clear, WeatherCodeMapper.labelFor(1))
        assertEquals(WeatherCodeMapper.iconFor(0), WeatherCodeMapper.iconFor(1))
    }

    @Test
    fun `kode yang muncul di response BMKG nyata terpetakan`() {
        assertEquals(R.string.weather_partly_cloudy, WeatherCodeMapper.labelFor(2))
        assertEquals(R.string.weather_cloudy, WeatherCodeMapper.labelFor(3))
        assertEquals(R.string.weather_rain, WeatherCodeMapper.labelFor(61))
    }

    @Test
    fun `kode gerimis Open-Meteo terpetakan`() {
        // 51 muncul di response Open-Meteo nyata untuk lokasi yang sama.
        assertEquals(R.string.weather_drizzle, WeatherCodeMapper.labelFor(51))
    }

    @Test
    fun `kode tak dikenal jatuh ke default tanpa exception`() {
        assertEquals(R.string.weather_unknown, WeatherCodeMapper.labelFor(999))
        assertNotNull(WeatherCodeMapper.iconFor(999))
    }

    @Test
    fun `kode null jatuh ke default tanpa exception`() {
        assertEquals(R.string.weather_unknown, WeatherCodeMapper.labelFor(null))
        assertNotNull(WeatherCodeMapper.iconFor(null))
    }

    @Test
    fun `seluruh rentang kode WMO tidak melempar exception`() {
        // Jaring pengaman: BMKG bisa memperkenalkan kode baru kapan saja dan
        // itu tidak boleh membuat layar cuaca gagal render.
        (0..99).forEach { code ->
            assertNotNull(WeatherCodeMapper.iconFor(code))
            assertNotNull(WeatherCodeMapper.labelFor(code))
        }
    }
}
