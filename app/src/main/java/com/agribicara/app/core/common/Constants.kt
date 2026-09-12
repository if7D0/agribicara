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

    /**
     * Jaring pengaman panjang jawaban, DI SISI MODEL.
     *
     * [AI_MAX_SENTENCES] hanya diminta lewat prompt, dan permintaan bisa
     * diabaikan. Ketika itu terjadi, TTS membacakan jawaban panjang sampai
     * habis tanpa tombol berhenti — petani menunggu tanpa bisa menyela.
     *
     * 300, bukan angka yang pas-pasan. Tiga kalimat Bahasa Indonesia yang wajar
     * berkisar 60-80 kata; dengan ~2 token per kata, itu sekitar 160 token.
     * Batas ini memberi ruang gerak dua kali lipat DENGAN SENGAJA: memotong
     * jawaban yang berperilaku baik di tengah kalimat jauh lebih buruk daripada
     * membiarkan jawaban nakal sedikit lebih panjang. Ini pengaman terhadap
     * kasus liar, bukan alat penegak gaya bahasa.
     */
    const val AI_MAX_OUTPUT_TOKENS = 300

    /**
     * Batas panjang pertanyaan yang diteruskan ke model.
     *
     * Ucapan manusia yang wajar jauh di bawah ini — satu kalimat pertanyaan
     * petani biasanya di bawah 100 karakter, dan hasil STT untuk satu tarikan
     * napas jarang melewati 200. Batas ini bukan untuk mereka.
     *
     * Gunanya menahan teks raksasa yang ditempel ke kolom input membanjiri
     * instruksi di atasnya. Model bekerja pada jendela terbatas: pertanyaan
     * yang cukup panjang bisa mendorong aturan menjawab keluar dari perhatian
     * model, dan aturan itulah yang menahannya menyebut dosis pestisida.
     */
    const val AI_MAX_QUESTION_CHARS = 500

    /**
     * Ditempelkan ke pertanyaan yang benar-benar dipotong [AI_MAX_QUESTION_CHARS].
     *
     * Tanpa ini pemotongan bersifat senyap: model menerima separuh kalimat dan
     * menjawabnya seolah pertanyaan utuh. Jawaban atas pertanyaan yang bukan
     * pertanyaan petani jauh lebih menyesatkan daripada jawaban yang mengakui
     * pertanyaannya terpotong.
     *
     * Berbahasa Indonesia karena seluruh prompt berbahasa Indonesia;
     * menyisipkan penanda berbahasa Inggris justru menarik model keluar dari
     * bahasa jawabannya. Sengaja TIDAK memakai `<<<`/`>>>` — keduanya dibuang
     * oleh sanitasi pertanyaan, sehingga penandanya akan lenyap.
     */
    const val QUESTION_TRUNCATED_MARKER = " … (pertanyaan dipotong karena terlalu panjang)"

    /** Berapa kali panggilan AI diulang setelah kegagalan sesaat. */
    const val AI_RETRY_COUNT = 1

    /**
     * Kedalaman maksimum rantai `cause` yang ditelusuri saat memilih pesan.
     *
     * Tanpa batas ini, `generateSequence(failure) { it.cause }` berputar
     * SELAMANYA pada rantai siklik — A.cause = B, B.cause = A. Itu bukan
     * kemustahilan teoretis: exception yang dibungkus ulang oleh dua lapisan
     * yang saling membungkus menghasilkannya, dan akibatnya bukan pesan yang
     * keliru melainkan aplikasi yang menggantung di dalam penanganan kegagalan
     * — kegagalan di dalam kegagalan, tempat paling buruk untuk hang.
     *
     * Sepuluh jauh di atas kedalaman mana pun yang pernah terlihat di proyek
     * ini (terdalam: SDK membungkus IOException, dua tingkat).
     */
    const val AI_CAUSE_CHAIN_LIMIT = 10

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
     * Panjang satu slot prakiraan BMKG, dalam jam.
     *
     * BMKG mengirim prakiraan per 3 jam (lihat `data/mapper/BmkgMapper`).
     * Dipakai [com.agribicara.app.domain.weather.ExtremeWeatherRule] untuk
     * memutuskan slot mana yang sudah lewat: sebuah slot baru dianggap
     * selesai setelah awalnya ditambah durasi ini, sehingga badai yang sedang
     * berlangsung tidak ikut terbuang bersama badai yang sudah berakhir.
     */
    const val FORECAST_SLOT_HOURS = 3L

    /**
     * Id baris tunggal tabel `sent_alert`.
     *
     * Alasannya sama dengan [USER_PREFERENCE_ID]: hanya ada satu peringatan
     * terakhir yang perlu diingat, dan baris tetap ber-id konstan membuat
     * penulisannya idempoten tanpa perlu query pencarian.
     */
    const val SENT_ALERT_ID = 1

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

    // --- Fase 4: deteksi penyakit padi -------------------------------------

    /**
     * Berkas model TFLite yang DIBUNDEL di assets aplikasi.
     *
     * Firebase ML Model Hosting (rencana awal) sudah deprecated (shutdown Juni
     * 2027) dan UI unggahnya dihapus dari Console, jadi model di-bundel langsung
     * di `app/src/main/assets/` dan dimuat on-device — tanpa jaringan, tanpa
     * backend. Dihasilkan di Task 0 (lihat `ml/`). Bila berkas tidak ada,
     * classifier melempar dan aplikasi menampilkan "model belum tersedia".
     */
    const val DISEASE_MODEL_ASSET = "rice_disease_classifier.tflite"

    /**
     * Berkas label di assets, satu label per baris, URUTANNYA harus sama dengan
     * urutan output model. Dihasilkan bersama model di Task 0. Bila berkas tidak
     * ada, classifier melempar dan repository memetakannya ke pesan "model belum
     * tersedia" — BUKAN menebak label.
     */
    const val DISEASE_LABELS_ASSET = "disease_labels.txt"

    /**
     * Atribusi dataset Paddy Doctor, dibundel sebagai aset.
     *
     * Salinan VERBATIM dari `ml/NOTICE`, bukan teks yang ditulis ulang di
     * `strings.xml`. Lisensi Apache 2.0 dataset mewajibkan atribusi ikut
     * disertakan pada distribusi aplikasi, dan kewajiban seperti itu tidak boleh
     * bergantung pada seseorang mengingat untuk menyalin ulang secara manual.
     *
     * `LicenseNoticeInvariantTest` memerahkan build bila aset ini menyimpang
     * dari `ml/NOTICE`, dan `app/build.gradle.kts` mendaftarkan kedua berkas
     * sebagai input task test supaya penjaganya tidak dilewati diam-diam oleh
     * pemeriksaan up-to-date Gradle.
     */
    const val LICENSE_NOTICE_ASSET = "paddy_doctor_notice.txt"

    /**
     * Sisi input model (piksel). 224 adalah lazim untuk MobileNet/EfficientNet-Lite.
     * WAJIB cocok dengan model hasil Task 0; bila model dilatih pada ukuran lain,
     * cukup ubah baris ini.
     */
    const val DISEASE_MODEL_INPUT_SIZE = 224

    /**
     * Normalisasi piksel sebelum inferensi: (piksel - MEAN) / STD.
     *
     * MEAN 0, STD 1 → mengirim piksel MENTAH [0..255] apa adanya. Ini WAJIB
     * cocok dengan pipeline pelatihan `ml/train.py`, yang memakai MobileNetV3
     * dengan `include_preprocessing=True` — model itu melakukan normalisasinya
     * sendiri dari input [0..255]. Salah normalisasi (mis. mengirim [0..1] ke
     * model yang menunggu [0..255]) menghasilkan prediksi yang terlihat yakin
     * tapi ngawur — persis bahaya yang dijaga gerbang keyakinan. Bila kelak
     * modelnya dilatih dengan pra-proses berbeda, ubah dua baris ini agar cocok.
     */
    const val DISEASE_INPUT_MEAN = 0f
    const val DISEASE_INPUT_STD = 1f

    /**
     * Ambang keyakinan minimum agar sebuah prediksi ditampilkan sebagai dugaan.
     *
     * Di bawah ini hasilnya "belum yakin", BUKAN diagnosis. Ini inti pengaman
     * fase ini: model closed-set akan memaksa satu label untuk foto apa pun
     * (daun sehat, penyakit di luar kelas, tangan, foto buram), dan diagnosis
     * yakin-tapi-salah bisa membuat petani salah bertindak.
     *
     * DIUKUR, bukan ditebak. Dari tabel kalibrasi `ml/train.py` pada model
     * val_accuracy 0.862 (dataset validasi Paddy Doctor):
     *
     *   ambang   foto dapat dugaan   dugaan itu benar
     *     0.55         91.2%              90.4%
     *     0.60         88.5%              91.7%   <- dipakai
     *     0.65         85.8%              92.9%
     *     0.70         83.0%              93.9%
     *     0.80         75.2%              96.2%
     *
     * Skrip merekomendasikan 0.55 (ambang terendah yang masih 90%), tetapi itu
     * tidak menyisakan cadangan: foto HP sungguhan skornya di bawah foto
     * validasi. Buktinya foto uji daun blas yang jelas hanya mendapat 0.6439 —
     * label BENAR, tapi tertolak ambang 0.70 maupun 0.65. 0.60 adalah ambang
     * paling ketat yang masih menerima kasus lapangan seperti itu, sambil
     * menahan ketepatan di 91.7% dan tetap lebih ketat dari saran skrip.
     *
     * Kalau model diganti, JALANKAN ULANG kalibrasi — angka ini milik model
     * tertentu, bukan konstanta universal.
     */
    const val DISEASE_CONFIDENCE_THRESHOLD = 0.60f

    /**
     * Ambang kedua: di atas ini hasil disebut dugaan KUAT, di bawahnya (tetapi
     * masih di atas [DISEASE_CONFIDENCE_THRESHOLD]) dugaan LEMAH.
     *
     * Sebelum ada pita ini, hasil 0.61 dan 0.97 tampil dengan kartu yang sama;
     * satu-satunya pembeda adalah angka persen kecil, yang justru paling tidak
     * terbaca oleh pembaca yang dituju aplikasi ini.
     *
     * DIUKUR dari tabel kalibrasi yang sama seperti ambang pertama, tetapi
     * dibaca MARGINAL per pita, bukan kumulatif. Angka kumulatif menyamarkan
     * masalahnya: ketepatan ">= 0.60" yang 91.7% itu ditopang hampir seluruhnya
     * oleh foto berkeyakinan tinggi. Dipecah per pita
     * (cakupan_i − cakupan_i+1 dan benar_i − benar_i+1):
     *
     *   pita           cakupan   dugaan itu benar
     *   0.60 – 0.65      2.7%          53.6%
     *   0.65 – 0.70      2.8%          63.3%
     *   0.70 – 0.80      7.8%          71.7%
     *   0.60 – 0.80     13.3%          66.3%   <- digabung jadi LEMAH
     *   >= 0.80         75.2%          96.2%   <- KUAT
     *
     * Jadi 0.80 bukan angka bulat yang enak dilihat: di situlah ketepatan
     * melompat dari dua-dari-tiga ke hampir pasti. Menaikkannya ke 0.90 hanya
     * memindahkan foto yang sudah 96% benar ke label "lemah"; menurunkannya ke
     * 0.70 menyebut "kuat" sesuatu yang meleset satu dari tiga kali.
     *
     * Pita LEMAH sengaja TIDAK disembunyikan — 13.3% foto ada di sana, dan
     * membuangnya berarti menukar dugaan lemah dengan layar "belum yakin" yang
     * tidak menolong siapa pun. Yang berubah hanya cara menyampaikannya.
     *
     * Sama seperti ambang pertama: kalau model diganti, JALANKAN ULANG
     * kalibrasi. Angka ini milik model tertentu.
     */
    const val DISEASE_STRONG_CONFIDENCE_THRESHOLD = 0.80f

    /** Batas baris riwayat deteksi yang diobservasi UI dan disimpan. */
    const val DETECTION_HISTORY_LIMIT = 50
}
