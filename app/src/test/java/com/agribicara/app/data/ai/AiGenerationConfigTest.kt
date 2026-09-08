package com.agribicara.app.data.ai

import com.agribicara.app.core.common.Constants
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.generationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memaku jaring pengaman panjang jawaban.
 *
 * Batas tiga kalimat hanya DIMINTA lewat prompt, dan permintaan bisa diabaikan
 * model. Ketika itu terjadi, TTS membacakan jawaban panjang sampai habis tanpa
 * tombol berhenti — petani menunggu tanpa bisa menyela. `maxOutputTokens`
 * memaksakannya di sisi model.
 *
 * Alasan test ini ada sama dengan [AiRequestOptionsTest]: menghapus
 * `generationConfig` dari [FirebaseTextGenerator] tetap compile, tetap lolos
 * lint, dan tetap lolos seluruh test lain. Tanpa penjaga, pengaman senyap
 * seperti ini hilang tanpa satu pun tanda.
 *
 * **Batas penjaga ini, dinyatakan terus terang.** Yang dibuktikan adalah objek
 * konfigurasi yang dibangun BENAR-BENAR membawa 300 token. Yang TIDAK
 * dibuktikan adalah bahwa [FirebaseTextGenerator] meneruskannya ke
 * `generativeModel` — itu menyentuh SDK Firebase dan tidak bisa dijalankan di
 * JVM. Menghapus baris `generationConfig = aiGenerationConfig()` di sana masih
 * akan lolos test ini.
 */
class AiGenerationConfigTest {

    /**
     * Membaca `maxOutputTokens` lewat refleksi.
     *
     * Getter-nya ada tetapi bernama-mangle `internal`
     * (`getMaxOutputTokens$com_google_firebase_ai_logic_firebase_ai`) sehingga
     * tidak bisa dipanggil dari modul ini.
     *
     * Isinya `java.lang.Integer` BIASA. Sengaja dicatat: jangan menyalin
     * `raw shr 1` dari [AiRequestOptionsTest] — pergeseran bit di sana khusus
     * untuk `kotlin.time.Duration` yang terkemas, dan menerapkannya di sini
     * menghasilkan 150, angka yang terlihat masuk akal dan salah.
     */
    private fun maxOutputTokens(config: GenerationConfig): Int? {
        val field = GenerationConfig::class.java.getDeclaredField("maxOutputTokens")
        field.isAccessible = true
        return field.get(config) as Int?
    }

    @Test
    fun `konfigurasi membawa batas 300 token keluaran`() {
        assertEquals(Constants.AI_MAX_OUTPUT_TOKENS, maxOutputTokens(aiGenerationConfig()))
    }

    @Test
    fun `SDK tidak membatasi apa pun bila konfigurasi dibiarkan kosong`() {
        // Tripwire, pola yang sama dengan AiRequestOptionsTest: membuktikan
        // nilainya BENAR-BENAR diisi, bukan kebetulan sama dengan bawaan.
        // Bawaan SDK adalah null — artinya tanpa aiGenerationConfig() panjang
        // jawaban tidak dibatasi sama sekali di sisi model, dan satu-satunya
        // penahan tinggal kalimat di dalam prompt.
        assertNull(maxOutputTokens(generationConfig { }))
    }

    @Test
    fun `batas memberi ruang gerak di atas tiga kalimat yang wajar`() {
        // Tiga kalimat Bahasa Indonesia yang wajar ~60-80 kata, sekitar 160
        // token. Batas ini SENGAJA longgar: memotong jawaban yang berperilaku
        // baik di tengah kalimat jauh lebih buruk daripada membiarkan jawaban
        // nakal sedikit lebih panjang. Kalau suatu saat seseorang mengetatkan
        // angkanya sampai mepet, test ini yang memberi tahu alasannya.
        assertTrue(
            "Batas terlalu ketat; jawaban wajar akan terpotong di tengah kalimat",
            Constants.AI_MAX_OUTPUT_TOKENS >= 250,
        )
    }
}
