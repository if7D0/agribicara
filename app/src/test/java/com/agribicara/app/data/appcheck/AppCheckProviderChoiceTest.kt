package com.agribicara.app.data.appcheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Memaku invarian keamanan App Check.
 *
 * Sebelum Fase 8, pilihan provider ditulis langsung sebagai `if (BuildConfig.DEBUG)`
 * di dalam `AgriBicaraApp` — kelas yang tidak bisa dijalankan di JVM, tidak
 * terhitung Kover, dan karena itu tidak pernah diuji sama sekali. Menukar
 * cabangnya secara tidak sengaja akan tetap compile, tetap lolos lint, dan tetap
 * lolos seluruh test lain, sementara akibatnya adalah App Check yang bisa
 * dipalsukan siapa pun yang membongkar APK rilis.
 *
 * **Batas penjaga ini, dinyatakan terus terang.** Yang dibuktikan adalah
 * [AppCheckProviderChoice] memetakan jenis build ke provider yang benar. Yang
 * TIDAK dibuktikan adalah bahwa `AgriBicaraApp.installAppCheck()` benar-benar
 * memanggilnya, dan bahwa `BuildConfig.DEBUG` yang diteruskan memang milik build
 * yang sedang berjalan — keduanya menyentuh Android dan SDK Firebase. Mengganti
 * pemanggilnya menjadi `forBuild(true)` secara hardcode masih akan lolos test ini.
 */
class AppCheckProviderChoiceTest {

    @Test
    fun `build debug memakai debug provider`() {
        assertEquals(AppCheckProvider.DEBUG, AppCheckProviderChoice.forBuild(isDebugBuild = true))
    }

    @Test
    fun `build rilis memakai Play Integrity`() {
        assertEquals(
            AppCheckProvider.PLAY_INTEGRITY,
            AppCheckProviderChoice.forBuild(isDebugBuild = false),
        )
    }

    @Test
    fun `build rilis TIDAK PERNAH memakai debug provider`() {
        // Sengaja terpisah dari test di atas meskipun terlihat mubazir. Yang di
        // atas menyatakan apa yang benar; yang ini menyatakan apa yang berbahaya,
        // dan pesan gagalnya menjelaskan akibatnya kepada siapa pun yang kelak
        // memerahkan test ini.
        assertNotEquals(
            "Build rilis memilih debug provider. App Check menjadi bisa dipalsukan " +
                "siapa pun yang membongkar APK, dan tidak ada lint atau build yang " +
                "akan mengeluh.",
            AppCheckProvider.DEBUG,
            AppCheckProviderChoice.forBuild(isDebugBuild = false),
        )
    }

    @Test
    fun `kedua jenis build menghasilkan provider yang berbeda`() {
        // Menangkap kekeliruan yang tidak tertangkap dua test di atas bila
        // keduanya kelak diubah bersamaan: fungsi yang mengabaikan argumennya
        // dan selalu mengembalikan nilai yang sama.
        assertNotEquals(
            AppCheckProviderChoice.forBuild(isDebugBuild = true),
            AppCheckProviderChoice.forBuild(isDebugBuild = false),
        )
    }
}
