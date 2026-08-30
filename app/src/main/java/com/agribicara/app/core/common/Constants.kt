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

    // --- Fase 3: suara -----------------------------------------------------

    /**
     * Bahasa untuk STT dan TTS.
     *
     * Satu tag BCP-47 untuk dua-duanya: RecognizerIntent.EXTRA_LANGUAGE
     * menerimanya apa adanya, dan TextToSpeech memakai
     * Locale.forLanguageTag(tag yang sama). Satu sumber kebenaran, sehingga
     * STT dan TTS tidak mungkin berbeda bahasa.
     */
    const val SPEECH_LANGUAGE_TAG = "id-ID"

    /**
     * Batas tunggu inisialisasi TextToSpeech.
     *
     * TextToSpeech.onInit bersifat asinkron dan pada sebagian perangkat TIDAK
     * PERNAH dipanggil bila engine-nya bermasalah. Tanpa batas ini layar suara
     * menggantung selamanya di keadaan "menyiapkan".
     */
    const val TTS_INIT_TIMEOUT_MS = 5_000L

    /** Jumlah alternatif hasil pengenalan; hanya yang teratas dipakai. */
    const val SPEECH_MAX_RESULTS = 1

    // --- Fase 5: AI --------------------------------------------------------

    /**
     * Model Gemini yang dipakai lewat Firebase AI Logic.
     *
     * flash-lite dipilih karena jawaban dibatasi 3 kalimat — kapasitas model
     * besar tidak terpakai, sedangkan latensi dan kuota terasa langsung bagi
     * petani dengan sinyal lemah. Bila kualitas Bahasa Indonesia ternyata
     * kurang, naikkan ke "gemini-3.7-flash": cukup mengubah baris ini.
     *
     * JANGAN memakai "gemini-2.0-flash" — sudah deprecated/shutdown, dan
     * masih banyak muncul di contoh kode lama.
     */
    const val GEMINI_MODEL = "gemini-3.5-flash-lite"

    /**
     * Batas panjang jawaban. Petani mendengarkan, bukan membaca: paragraf
     * panjang lewat TTS mustahil diingat.
     */
    const val AI_MAX_SENTENCES = 3

    /** Berapa kali panggilan AI diulang setelah kegagalan sesaat. */
    const val AI_RETRY_COUNT = 1

    /**
     * Batas tunggu satu panggilan AI.
     *
     * Bawaan SDK adalah 180 detik (RequestOptions.timeoutInMillis, firebase-ai
     * 17.16.0) — terlalu panjang untuk sinyal desa: dengan [AI_RETRY_COUNT],
     * layar bisa tertahan enam menit tanpa tombol batal, dan seluruh tangga
     * degradasi (pesan kegagalan, jawaban tersimpan, tombol "lihat cuaca
     * saja") antre di belakangnya. Dengan 30 detik, kasus terburuk menjadi
     * satu menit dan petani cepat mendapat sesuatu yang bisa ditindaklanjuti.
     */
    const val AI_TIMEOUT_MS = 30_000L

    /**
     * Umur maksimum jawaban tersimpan yang masih boleh dipakai ulang.
     *
     * Disamakan dengan [CACHE_STALE_HOURS] dengan sengaja: jawaban ini
     * di-grounding data cuaca, jadi tidak masuk akal bertahan lebih lama
     * daripada cuaca yang mendasarinya.
     */
    const val ANSWER_CACHE_HOURS = 6L

    /** Batas baris riwayat yang diobservasi UI. */
    const val CHAT_HISTORY_LIMIT = 50

    /**
     * Berapa hari prakiraan yang disisipkan ke prompt.
     *
     * Dibatasi 3 karena hanya hari 1-3 yang benar-benar dari BMKG; hari 4-7
     * adalah estimasi Open-Meteo dan tidak layak dijadikan dasar saran
     * bertindak.
     */
    const val PROMPT_FORECAST_DAYS = 3

    // --- Fase 6: notifikasi cuaca ekstrem ----------------------------------

    /**
     * Curah hujan sehari yang dianggap ekstrem.
     *
     * 50 mm/hari adalah batas bawah kategori "hujan lebat" BMKG. Sengaja
     * konservatif: notifikasi yang terlalu sering justru diabaikan, dan
     * peringatan yang diabaikan sama tidak bergunanya dengan tidak ada.
     */
    const val EXTREME_RAIN_MM_PER_DAY = 50.0

    /**
     * Berapa hari ke depan yang diperiksa untuk peringatan.
     *
     * Dibatasi 2 karena hanya hari-hari awal yang berasal dari BMKG; hari
     * jauh adalah estimasi Open-Meteo dan tidak layak membangunkan petani.
     */
    const val ALERT_LOOKAHEAD_DAYS = 2

    /**
     * Jarak antar pemeriksaan cuaca di latar belakang.
     *
     * Disamakan dengan [CACHE_STALE_HOURS]: memeriksa lebih sering hanya akan
     * membaca cache yang sama tanpa data baru. WorkManager punya interval
     * minimum 15 menit dan Doze dapat menundanya berjam-jam di perangkat
     * murah — ini peringatan dini yang mungkin telat, BUKAN alarm.
     */
    const val WEATHER_CHECK_INTERVAL_HOURS = 6L

    /** Id channel notifikasi cuaca ekstrem. Tidak boleh berubah setelah rilis. */
    const val WEATHER_ALERT_CHANNEL_ID = "weather_alert"

    /** Satu notifikasi cuaca menggantikan yang sebelumnya, tidak menumpuk. */
    const val WEATHER_ALERT_NOTIFICATION_ID = 1001

    /** Nama pekerjaan periodik; dipakai untuk enqueueUniquePeriodicWork. */
    const val WEATHER_CHECK_WORK_NAME = "weather_check"
}
