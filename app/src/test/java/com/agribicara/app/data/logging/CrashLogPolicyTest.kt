package com.agribicara.app.data.logging

import android.util.Log
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memaku kebijakan penyaringan log yang menuju Crashlytics.
 *
 * Dua hal yang dijaga, dan keduanya berpihak pada petani. Pertama, kuota data:
 * meneruskan VERBOSE/DEBUG/INFO berarti mengirim paket justru saat sinyal sedang
 * buruk. Kedua, keterbacaan laporan: mengangkat setiap WARN menjadi non-fatal
 * akan menenggelamkan Crashlytics sampai crash sungguhan tidak terlihat lagi.
 *
 * Konstanta [Log] aman dipakai di unit test JVM karena `Log.WARN` dan
 * kawan-kawannya adalah `static final int` — nilainya di-inline compiler dan
 * tidak ada satu pun metode Android yang dipanggil. Ini alasan
 * [CrashLogPolicy] sengaja tidak menyentuh apa pun dari Android selain konstanta.
 */
class CrashLogPolicyTest {

    @Test
    fun `WARN diteruskan`() {
        assertTrue(CrashLogPolicy.shouldReport(Log.WARN))
    }

    @Test
    fun `ERROR diteruskan`() {
        assertTrue(CrashLogPolicy.shouldReport(Log.ERROR))
    }

    @Test
    fun `ASSERT diteruskan`() {
        // Log.wtf berada DI ATAS ERROR dan menandai keadaan yang menurut kode
        // seharusnya mustahil. Kalau ia dibuang, justru laporan yang paling
        // perlu dilihat yang hilang. Diputuskan eksplisit, bukan jatuh ke else.
        assertTrue(CrashLogPolicy.shouldReport(Log.ASSERT))
    }

    @Test
    fun `VERBOSE dan DEBUG dan INFO dibuang`() {
        listOf(Log.VERBOSE, Log.DEBUG, Log.INFO).forEach { priority ->
            assertFalse(
                "priority=$priority diteruskan ke Crashlytics. Di build rilis level " +
                    "ini tidak pernah ditanam, dan mengirimnya hanya menghabiskan " +
                    "kuota data petani saat sinyal sedang buruk.",
                CrashLogPolicy.shouldReport(priority),
            )
        }
    }

    @Test
    fun `hanya ERROR dan ASSERT yang menjadi non-fatal`() {
        assertTrue(CrashLogPolicy.shouldRecordAsNonFatal(Log.ERROR))
        assertTrue(CrashLogPolicy.shouldRecordAsNonFatal(Log.ASSERT))
    }

    @Test
    fun `WARN dicatat sebagai jejak tetapi bukan non-fatal`() {
        // Pasangan yang membedakan dua fungsi itu. Kalau keduanya kelak
        // disatukan menjadi satu kebijakan, test inilah yang memerah.
        assertTrue(CrashLogPolicy.shouldReport(Log.WARN))
        assertFalse(
            "Setiap WARN menjadi laporan non-fatal. Crashlytics akan tenggelam " +
                "oleh kebisingan sampai crash sungguhan tidak terlihat lagi.",
            CrashLogPolicy.shouldRecordAsNonFatal(Log.WARN),
        )
    }

    @Test
    fun `level di luar rentang yang dikenal dibuang`() {
        // Timber meneruskan angka apa adanya. Nilai di luar 2..7 tidak pernah
        // datang dari Android, tetapi kebijakan yang memakai perbandingan
        // ">= WARN" akan meloloskannya diam-diam — dan implementasi seperti itu
        // adalah refactor yang sangat mungkin terjadi.
        listOf(0, 1, 99, -5).forEach { priority ->
            assertFalse(
                "priority=$priority diteruskan. Kebijakan ini harus mendaftar level " +
                    "yang diterima, bukan membandingkan besar-kecil.",
                CrashLogPolicy.shouldReport(priority),
            )
        }
    }
}
