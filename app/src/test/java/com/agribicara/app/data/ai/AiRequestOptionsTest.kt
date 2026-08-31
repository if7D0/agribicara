package com.agribicara.app.data.ai

import com.agribicara.app.core.common.Constants
import com.google.firebase.ai.type.RequestOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memaku timeout panggilan AI.
 *
 * Tanpa `requestOptions`, SDK Firebase memakai bawaannya: **180 detik**. Petani
 * di desa bersinyal buruk akan menunggu tiga menit sebelum tahu panggilannya
 * gagal, dan tidak ada satu pun pemeriksaan otomatis yang akan mengeluh —
 * menghapusnya tetap compile, tetap lolos lint, tetap lolos seluruh test lain.
 * Sebelumnya nilai ini hanya pernah dibuktikan dengan membaca disassembly kelas
 * rilis secara manual: bukti sekali pakai, bukan penjaga.
 *
 * **Batas penjaga ini, dinyatakan terus terang.** Yang dibuktikan adalah objek
 * opsi yang dibangun BENAR-BENAR membawa 30 detik. Yang TIDAK dibuktikan adalah
 * bahwa `FirebaseTextGenerator` meneruskannya ke `generativeModel` — itu
 * menyentuh SDK Firebase dan tidak bisa dijalankan di JVM. Menghapus baris
 * `requestOptions = aiRequestOptions()` di sana masih akan lolos test ini.
 */
class AiRequestOptionsTest {

    /**
     * Membaca field `timeout` milik [RequestOptions] lewat refleksi.
     *
     * `RequestOptions` tidak mengekspos timeout-nya sebagai properti yang bisa
     * dibaca — parameter konstruktornya bukan `val`.
     *
     * Isinya adalah `kotlin.time.Duration` dalam bentuk TERKEMAS, bukan angka
     * milidetik biasa: nilainya digeser satu bit ke kiri, dan bit terendah
     * menandakan satuannya (0 = nanodetik, 1 = milidetik). Ini diverifikasi
     * secara empiris, bukan diasumsikan — `RequestOptions()` bawaan menyimpan
     * 360000000000, yang setelah digeser menjadi 180 detik, persis angka
     * bawaan SDK yang disebut KDoc `Constants.AI_TIMEOUT_MS`.
     */
    private fun timeoutMillis(options: RequestOptions): Long {
        val field = RequestOptions::class.java.getDeclaredField("timeout")
        field.isAccessible = true
        val raw = field.getLong(options)

        val nilai = raw shr 1
        val dalamMilidetik = (raw and 1L) == 1L
        return if (dalamMilidetik) nilai else nilai / 1_000_000
    }

    @Test
    fun `opsi request membawa timeout 30 detik`() {
        assertEquals(Constants.AI_TIMEOUT_MS, timeoutMillis(aiRequestOptions()))
    }

    @Test
    fun `timeout berbeda dari bawaan SDK yang 180 detik`() {
        // Membuktikan opsinya BENAR-BENAR diisi, bukan kebetulan sama dengan
        // bawaan. Bawaan SDK dibaca dari SDK-nya sendiri, bukan ditulis ulang
        // sebagai angka, supaya kalau Firebase mengubahnya test ini ikut tahu.
        val bawaanSdk = timeoutMillis(RequestOptions())

        assertEquals(180_000L, bawaanSdk)
        assertNotEquals(bawaanSdk, timeoutMillis(aiRequestOptions()))
        assertTrue(
            "Timeout tidak boleh mendekati bawaan SDK; petani menunggu selama itu",
            timeoutMillis(aiRequestOptions()) < bawaanSdk,
        )
    }
}
