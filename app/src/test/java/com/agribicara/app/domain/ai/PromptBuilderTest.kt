package com.agribicara.app.domain.ai

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.WeatherSource
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private val today = LocalDate.of(2026, 8, 30) // Minggu

    private fun day(
        date: LocalDate,
        max: Double? = 31.0,
        min: Double? = 24.0,
        rain: Double? = 2.0,
        description: String? = "Berawan",
    ) = DailyForecast(
        date = date,
        temperatureMax = max,
        temperatureMin = min,
        precipitationMm = rain,
        windSpeed = 5.0,
        weatherCode = 3,
        description = description,
        source = WeatherSource.BMKG,
    )

    private fun forecast(days: List<DailyForecast>) = Forecast(
        regionCode = "11.01.01.2001",
        regionName = "Keude Bakongan",
        days = days,
        source = WeatherSource.BMKG,
        fetchedAt = 0L,
    )

    @Test
    fun `prompt memuat pertanyaan petani apa adanya`() {
        val prompt = PromptBuilder.build("kapan waktu terbaik memupuk padi", null, today)

        assertTrue(prompt.contains("kapan waktu terbaik memupuk padi"))
    }

    @Test
    fun `prompt dengan cuaca memuat suhu kondisi dan nama hari`() {
        val prompt = PromptBuilder.build(
            question = "kapan memupuk padi",
            forecast = forecast(listOf(day(today), day(today.plusDays(1)))),
            today = today,
        )

        assertTrue("suhu maksimum hilang", prompt.contains("31"))
        assertTrue("suhu minimum hilang", prompt.contains("24"))
        assertTrue("kondisi cuaca hilang", prompt.contains("Berawan"))
        assertTrue("nama wilayah hilang", prompt.contains("Keude Bakongan"))
        // Hari disebut dengan NAMA, bukan tanggal — petani mendengar jawaban,
        // dan "Senin" jauh lebih mudah dipakai daripada "2026-08-31".
        // Diperiksa dengan awalan "- " supaya yang teruji adalah BARIS CUACA,
        // bukan kata yang kebetulan muncul di teks instruksi.
        assertTrue("baris hari ini hilang", prompt.contains("- hari ini:"))
        assertTrue("baris besok hilang", prompt.contains("- besok:"))
    }

    @Test
    fun `prompt tanpa cuaca menyatakan ketiadaannya secara eksplisit`() {
        val prompt = PromptBuilder.build("kapan memupuk padi", null, today)

        // Model yang tidak diberi tahu akan mengarang cuaca. Ketiadaan data
        // harus DINYATAKAN, bukan sekadar dihilangkan dari prompt.
        assertTrue(prompt.contains("tidak tersedia"))
    }

    @Test
    fun `prompt tanpa cuaca melarang model mengarang cuaca`() {
        val prompt = PromptBuilder.build("kapan memupuk padi", null, today)

        assertTrue(prompt.lowercase().contains("jangan"))
    }

    @Test
    fun `prompt membatasi jawaban tiga kalimat dan Bahasa Indonesia`() {
        val prompt = PromptBuilder.build("kapan memupuk padi", null, today)

        assertTrue(prompt.contains(Constants.AI_MAX_SENTENCES.toString()))
        assertTrue(prompt.contains("Bahasa Indonesia"))
    }

    @Test
    fun `prompt memerintahkan mengaku tidak tahu`() {
        val prompt = PromptBuilder.build("kapan memupuk padi", null, today)

        assertTrue(prompt.lowercase().contains("tidak tahu"))
    }

    @Test
    fun `prompt hanya memuat hari sebanyak batas yang ditetapkan`() {
        val days = (0L..6L).map { day(today.plusDays(it)) }
        val prompt = PromptBuilder.build("kapan memupuk padi", forecast(days), today)

        // Hari 4-7 adalah estimasi Open-Meteo, tidak layak jadi dasar saran.
        val hariKe4 = today.plusDays(3)
        assertFalse(
            "hari ke-4 seharusnya tidak masuk prompt",
            prompt.contains("- " + PromptBuilder.dayLabel(hariKe4, today) + ":"),
        )
    }

    @Test
    fun `hari yang sudah lewat tidak masuk prompt`() {
        val days = listOf(day(today.minusDays(2)), day(today), day(today.plusDays(1)))
        val prompt = PromptBuilder.build("kapan memupuk padi", forecast(days), today)

        assertFalse(prompt.contains(today.minusDays(2).toString()))
        // Sisa dua hari yang sah tetap ada.
        assertTrue(prompt.contains("- hari ini:"))
    }

    @Test
    fun `nilai cuaca yang kosong tidak menghasilkan null di prompt`() {
        val prompt = PromptBuilder.build(
            question = "kapan memupuk padi",
            forecast = forecast(listOf(day(today, max = null, min = null, rain = null, description = null))),
            today = today,
        )

        assertFalse("kata 'null' bocor ke prompt", prompt.contains("null"))
    }

    @Test
    fun `pertanyaan dengan spasi berlebih dirapikan`() {
        val prompt = PromptBuilder.build("   kapan   memupuk padi   ", null, today)

        assertTrue(prompt.contains("kapan memupuk padi"))
    }

    @Test
    fun `nama hari Bahasa Indonesia benar untuk seluruh minggu`() {
        // 2026-08-30 adalah Minggu. Hari ini dan besok punya label relatif
        // ("hari ini", "besok") secara sengaja, jadi nama hari baru berlaku
        // mulai lusa.
        val expected = mapOf(
            2L to "Selasa",
            3L to "Rabu",
            4L to "Kamis",
            5L to "Jumat",
            6L to "Sabtu",
        )
        expected.forEach { (offset, name) ->
            assertEquals(name, PromptBuilder.dayLabel(today.plusDays(offset), today))
        }
    }

    @Test
    fun `hari ini dan besok memakai label relatif bukan nama hari`() {
        assertEquals("hari ini", PromptBuilder.dayLabel(today, today))
        assertEquals("besok", PromptBuilder.dayLabel(today.plusDays(1), today))
    }
}
