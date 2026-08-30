package com.agribicara.app.core.common

/** Konstanta app-wide. Tidak boleh ada nilai hardcoded tersebar di kode. */
object Constants {

    /** Nama file database Room. */
    const val DATABASE_NAME = "agribicara.db"

    /** Timeout jaringan (detik). BMKG API kadang lambat, lihat catatan PRD. */
    const val NETWORK_TIMEOUT_SECONDS = 15L

    /** Baris tunggal pada tabel preferensi pengguna. */
    const val USER_PREFERENCE_ID = 1

    // --- Fase 2: cuaca -----------------------------------------------------

    /**
     * BMKG. Base URL Retrofit WAJIB diakhiri "/".
     *
     * Endpoint publik JSON, tanpa API key. Hanya menerima parameter `adm4`
     * (kode kelurahan/desa) — `adm3` ditolak dengan 301. Spike 2026-08-29.
     */
    const val BMKG_BASE_URL = "https://api.bmkg.go.id/"

    /** Open-Meteo, fallback saat BMKG gagal. Gratis, tanpa API key. */
    const val OPEN_METEO_BASE_URL = "https://api.open-meteo.com/"

    /**
     * Sumber kode wilayah untuk region picker.
     *
     * Formatnya persis kode yang diterima BMKG (mis. "32.77.01.1002"),
     * diverifikasi ujung-ke-ujung pada spike 2026-08-29.
     */
    const val WILAYAH_BASE_URL = "https://wilayah.id/"

    /** Panjang prakiraan yang ditampilkan (success signal PRD Fase 2). */
    const val FORECAST_DAYS = 7

    /**
     * BMKG hanya menyediakan ~3 hari (terverifikasi: 19 entri 3-jam-an).
     * Sisanya sampai [FORECAST_DAYS] ditambal Open-Meteo dan ditandai
     * sebagai estimasi.
     */
    const val BMKG_FORECAST_DAYS = 3

    /** Setelah sekian jam, data cache dianggap basi dan di-refresh bila online. */
    const val CACHE_STALE_HOURS = 6L

    /** Zona waktu default untuk permintaan Open-Meteo dan pengelompokan hari. */
    const val DEFAULT_TIMEZONE = "Asia/Jakarta"

    /** Daily field yang diminta ke Open-Meteo. */
    const val OPEN_METEO_DAILY_FIELDS =
        "temperature_2m_max,temperature_2m_min,precipitation_sum,windspeed_10m_max,weathercode"
}
