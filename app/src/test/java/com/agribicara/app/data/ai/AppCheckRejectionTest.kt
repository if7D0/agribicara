package com.agribicara.app.data.ai

import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pengenalan penolakan App Check dari exception SDK.
 *
 * Ada karena satu sesi penuh terbuang memburu fitur suara yang tampak mati
 * total padahal seluruh kodenya benar: debug token perangkat belum terdaftar,
 * dan pesan "layanan sedang bermasalah" menyembunyikannya di antara kuota habis
 * dan gangguan server.
 *
 * **Yang benar-benar diuji di sini adalah TEKS pesan SDK, bukan tipenya.**
 * `ServerException` tidak bisa dibuat dari test: konstruktornya `internal` di
 * Kotlin — kendala yang persis sama sudah dibayar untuk `RequestTimeoutException`
 * dan melahirkan [AiTimeoutException]. Itu tidak mengurangi nilai test ini,
 * karena [isAppCheckRejection] memang hanya membaca `message` dan menerima
 * [Throwable] apa pun; tipe exception tidak pernah ikut menentukan hasilnya.
 *
 * Yang penting justru dijaga ketat: string di [PESAN_ASLI_SDK] adalah salinan
 * verbatim dari logcat perangkat sungguhan, bukan karangan ulang. Di situlah
 * satu-satunya tempat test ini bisa berbohong.
 */
class AppCheckRejectionTest {

    private companion object {
        /**
         * Pesan APA ADANYA dari firebase-ai 17.16.0, disalin dari logcat
         * Infinix X6851 (2026-09-08 01:45:28) saat kegagalan sungguhan
         * direproduksi:
         *
         * ```
         * com.google.firebase.ai.type.ServerException:
         *   Firebase App Check token is invalid.
         * ```
         *
         * Kalau baris ini perlu diubah karena SDK dinaikkan, itu SINYAL:
         * pencocokan di [isAppCheckRejection] kemungkinan besar ikut perlu
         * ditinjau, dan cara memeriksanya adalah mereproduksi ulang di
         * perangkat — bukan menebak.
         */
        const val PESAN_ASLI_SDK = "Firebase App Check token is invalid."
    }

    @Test
    fun `pesan App Check asli dari SDK dikenali`() {
        assertTrue(isAppCheckRejection(RuntimeException(PESAN_ASLI_SDK)))
    }

    @Test
    fun `penolakan yang terbungkus exception lain tetap dikenali`() {
        // SDK membungkus kegagalan di dalam exception-nya sendiri, jadi rantai
        // `cause` HARUS ditelusuri — pelajaran yang sama sudah dibayar di
        // FirebaseAiRepositoryTest untuk IOException.
        val terbungkus = IllegalStateException("gagal", RuntimeException(PESAN_ASLI_SDK))

        assertTrue(isAppCheckRejection(terbungkus))
    }

    @Test
    fun `kegagalan server biasa TIDAK disalahartikan sebagai App Check`() {
        // Inilah yang membuat pencocokan teks pantas dipakai: ia harus lebih
        // sempit daripada "kegagalan server apa pun". Kuota habis bukan masalah
        // setelan, dan mengarahkan developer ke Manage debug tokens untuk itu
        // sama merugikannya dengan pesan generik yang diganti fungsi ini.
        assertFalse(isAppCheckRejection(RuntimeException("Quota exceeded.")))
    }

    @Test
    fun `kegagalan jaringan murni TIDAK dikenali sebagai App Check`() {
        assertFalse(isAppCheckRejection(IOException("socket tertutup")))
    }

    @Test
    fun `exception tanpa pesan tidak membuat crash`() {
        // message null adalah hal biasa pada exception yang dibuat SDK lain;
        // pencocokan yang lengah di sini berubah jadi NullPointerException di
        // tengah penanganan kegagalan — kegagalan di dalam kegagalan.
        assertFalse(isAppCheckRejection(RuntimeException()))
    }
}
