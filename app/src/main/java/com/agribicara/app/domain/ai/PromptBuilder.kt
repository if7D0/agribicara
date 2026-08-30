package com.agribicara.app.domain.ai

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.model.DailyForecast
import com.agribicara.app.domain.model.Forecast
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Menyusun prompt untuk Gemini dari pertanyaan petani + prakiraan cuaca.
 *
 * Objek murni tanpa dependensi Android, mengikuti gaya
 * [com.agribicara.app.data.speech.SpeechErrorMapper] dari Fase 3 (walau kelas
 * ini tinggal di domain karena isinya kebijakan, bukan akses data), sehingga
 * bisa diuji penuh di JVM. Ini kelas dengan risiko tertinggi di Fase 5:
 * prompt yang buruk menghasilkan saran pertanian yang salah, dan petani tidak
 * punya cara memverifikasinya.
 */
object PromptBuilder {

    /**
     * Nama hari Bahasa Indonesia ditulis eksplisit, TIDAK memakai
     * Locale("id") pada DateTimeFormatter.
     *
     * Alasannya: ketersediaan data locale berbeda antara JVM desktop (tempat
     * unit test berjalan) dan Android (tempat produksi berjalan), sehingga
     * formatter berbasis locale bisa lulus test lalu menghasilkan "Sunday" di
     * perangkat. Peta ini menghilangkan seluruh kelas kegagalan itu.
     */
    private val DAY_NAMES = mapOf(
        DayOfWeek.MONDAY to "Senin",
        DayOfWeek.TUESDAY to "Selasa",
        DayOfWeek.WEDNESDAY to "Rabu",
        DayOfWeek.THURSDAY to "Kamis",
        DayOfWeek.FRIDAY to "Jumat",
        DayOfWeek.SATURDAY to "Sabtu",
        DayOfWeek.SUNDAY to "Minggu",
    )

    /**
     * Label hari yang dipakai di prompt.
     *
     * Hari ini dan besok diberi label relatif karena itulah cara orang
     * berbicara; sisanya memakai nama hari. Tanggal ISO sengaja TIDAK dipakai:
     * jawaban akan dibacakan lewat TTS, dan "dua ribu dua puluh enam garis
     * miring delapan" tidak berguna bagi siapa pun.
     */
    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "hari ini"
        today.plusDays(1) -> "besok"
        else -> DAY_NAMES.getValue(date.dayOfWeek)
    }

    /**
     * @param question ucapan atau ketikan petani, mentah
     * @param forecast prakiraan wilayahnya, atau null bila tidak tersedia
     * @param today tanggal lokal, di-inject agar test deterministik
     */
    fun build(question: String, forecast: Forecast?, today: LocalDate): String {
        val cleanQuestion = collapseWhitespace(question)

        return buildString {
            appendLine(instructions())
            appendLine()
            appendLine(weatherSection(forecast, today))
            appendLine()
            append("Pertanyaan petani: ")
            append(cleanQuestion)
        }
    }

    private fun instructions(): String = """
        Kamu adalah penyuluh pertanian untuk petani kecil di Indonesia.

        Aturan menjawab:
        - Jawab dalam Bahasa Indonesia sederhana sehari-hari, tanpa istilah teknis.
        - Maksimum ${Constants.AI_MAX_SENTENCES} kalimat. Jawabanmu akan dibacakan
          dengan suara, bukan dibaca.
        - Sebut hari dengan namanya (Senin, Selasa) atau "hari ini"/"besok",
          jangan pernah memakai tanggal angka.
        - Bila kamu tidak tahu, katakan tidak tahu. Jangan menebak.
        - Jangan menyebut merek atau dosis pestisida maupun pupuk kimia secara
          spesifik; arahkan ke penyuluh setempat bila itu yang ditanyakan.
    """.trimIndent()

    private fun weatherSection(forecast: Forecast?, today: LocalDate): String {
        if (forecast == null) {
            // Ketiadaan data DINYATAKAN, bukan dihilangkan diam-diam. Model
            // yang tidak diberi tahu akan mengarang cuaca dengan percaya diri,
            // dan petani tidak punya cara membedakannya dari data asli.
            return "Data cuaca untuk wilayah petani ini tidak tersedia saat ini. " +
                "Jangan mengarang perkiraan cuaca. Jawab dari pengetahuan umum " +
                "pertanian saja, dan sebutkan bahwa kamu tidak punya data cuaca."
        }

        val days = forecast.days
            .filter { !it.date.isBefore(today) }
            .sortedBy { it.date }
            .take(Constants.PROMPT_FORECAST_DAYS)

        if (days.isEmpty()) {
            return "Data cuaca untuk ${forecast.regionName} sudah kedaluwarsa. " +
                "Jangan mengarang perkiraan cuaca."
        }

        return buildString {
            appendLine("Prakiraan cuaca untuk ${forecast.regionName}:")
            days.forEach { append(describeDay(it, today)) }
            append("Gunakan fakta cuaca di atas bila relevan dengan pertanyaan.")
        }
    }

    /** Satu baris fakta bernama, bukan JSON mentah — model membacanya lebih andal. */
    private fun describeDay(day: DailyForecast, today: LocalDate): String {
        val parts = buildList {
            day.description?.let { add(it) }
            // Suhu hanya ditulis bila keduanya ada; rentang setengah lengkap
            // ("suhu 31 sampai null") lebih membingungkan daripada dihilangkan.
            if (day.temperatureMax != null && day.temperatureMin != null) {
                add("suhu ${format(day.temperatureMin)} sampai ${format(day.temperatureMax)} derajat")
            }
            day.precipitationMm?.let { add("curah hujan ${format(it)} milimeter") }
        }

        val label = dayLabel(day.date, today)
        return if (parts.isEmpty()) {
            "- $label: data tidak lengkap\n"
        } else {
            "- $label: ${parts.joinToString(", ")}\n"
        }
    }

    /** 31.0 -> "31", 2.5 -> "2.5". Angka bulat tanpa ".0" lebih enak dibaca TTS. */
    private fun format(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

    /**
     * Merapikan spasi berlebih tanpa regex.
     *
     * Ditulis manual alih-alih Regex("[whitespace]+") secara sadar: bentuk
     * loop ini bebas escape sequence, dan ia menangani tab maupun baris baru
     * yang bisa ikut terbawa dari kolom teks.
     */
    private fun collapseWhitespace(text: String): String {
        val out = StringBuilder(text.length)
        var previousWasSpace = false
        for (character in text.trim()) {
            val isSpace = character.isWhitespace()
            if (isSpace) {
                if (!previousWasSpace) out.append(' ')
            } else {
                out.append(character)
            }
            previousWasSpace = isSpace
        }
        return out.toString()
    }
}
