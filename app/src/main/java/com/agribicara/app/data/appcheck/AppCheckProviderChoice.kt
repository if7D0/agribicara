package com.agribicara.app.data.appcheck

/**
 * Provider App Check yang boleh dipasang pada sebuah build.
 *
 * Sengaja enum, bukan Boolean: `installDebugProvider = false` di tempat
 * pemanggilan tidak memberi tahu apa yang dipasang sebagai gantinya, dan
 * pilihannya kelak bisa bertambah (mis. provider kustom untuk distribusi di
 * luar Play).
 */
enum class AppCheckProvider {
    /**
     * Hanya untuk build debug. Provider ini mencetak debug token ke logcat dan
     * token itu harus didaftarkan manual di Firebase console.
     */
    DEBUG,

    /**
     * Build rilis. Menyandarkan keabsahan pada Play Integrity API.
     *
     * Menuntut SHA-256 sertifikat penanda tangan terdaftar di Firebase. Dengan
     * Play App Signing, sertifikat yang benar-benar dipakai adalah milik Google,
     * BUKAN upload key — lihat `docs/play/release-checklist.md` butir B6–B8.
     */
    PLAY_INTEGRITY,
}

/**
 * Memilih provider App Check dari jenis build.
 *
 * Diangkat keluar dari [com.agribicara.app.AgriBicaraApp] karena alasan yang
 * sama dengan [com.agribicara.app.data.speech.SpeechErrorMapper]: kelas
 * `Application` tidak bisa dijalankan di JVM, sementara keputusan di dalamnya
 * adalah invarian KEAMANAN.
 *
 * Memasang debug provider di build rilis membuat App Check bisa dipalsukan siapa
 * pun yang membongkar APK — dan tidak ada lint, build, atau test lain di proyek
 * ini yang akan mengeluh. Kegagalannya sunyi, persis seperti `tools:node="remove"`
 * di Fase 6. Fungsi ini ada supaya kegagalan itu punya satu tempat yang bisa
 * diuji.
 */
object AppCheckProviderChoice {

    fun forBuild(isDebugBuild: Boolean): AppCheckProvider =
        if (isDebugBuild) AppCheckProvider.DEBUG else AppCheckProvider.PLAY_INTEGRITY
}
