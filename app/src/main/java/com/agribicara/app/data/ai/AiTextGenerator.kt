package com.agribicara.app.data.ai

/**
 * Batas tipis di atas SDK Firebase AI.
 *
 * Ada bukan demi abstraksi, melainkan demi bisa diuji. [FirebaseAiRepository]
 * memegang kebijakan yang salahnya langsung terasa oleh petani — berapa kali
 * mengulang, kapan berhenti mengulang, pesan mana yang muncul — sedangkan
 * membangun model Firebase menuntut FirebaseApp yang hidup dan tidak mungkin
 * dijalankan di unit test JVM. Dengan batas ini, kebijakan tersebut diuji
 * memakai generator palsu, dan kode yang benar-benar menyentuh SDK tinggal
 * sedikit sekali sehingga cukup diperiksa dengan mata.
 */
fun interface AiTextGenerator {

    /**
     * Mengirim [prompt] apa adanya ke model.
     *
     * @return teks mentah jawaban, atau null bila model tidak menghasilkan apa
     *   pun (mis. diblokir filter keamanan). Kegagalan lain dilempar sebagai
     *   exception; [FirebaseAiRepository] yang menerjemahkannya menjadi pesan.
     */
    suspend fun generate(prompt: String): String?
}

/**
 * Panggilan AI melewati [com.agribicara.app.core.common.Constants.AI_TIMEOUT_MS].
 *
 * Tipe sendiri, bukan tipe SDK: konstruktor exception timeout Firebase bersifat
 * `internal` sehingga tidak bisa dibuat dari test, dan membawa tipe SDK sampai
 * ke lapisan pemetaan pesan berarti mengikat kelas yang justru paling ingin
 * diuji kepada Firebase.
 */
class AiTimeoutException(cause: Throwable? = null) :
    Exception("Panggilan AI melewati batas waktu", cause)

/**
 * App Check menolak panggilan AI.
 *
 * Tipe sendiri dengan alasan yang sama seperti [AiTimeoutException], DITAMBAH
 * satu alasan yang khas: kegagalan ini hampir selalu berarti debug token
 * perangkat belum didaftarkan di Firebase Console — masalah SETELAN, bukan
 * masalah kode dan bukan masalah petani. Tanpa tipe tersendiri ia jatuh ke
 * keranjang "layanan bermasalah" bersama kuota habis dan gangguan server,
 * dan developer kehilangan satu-satunya petunjuk yang membedakannya.
 *
 * Pernah menghabiskan satu sesi penuh: fitur suara tampak mati total sementara
 * seluruh kodenya benar. Lihat `docs/play/release-checklist.md` butir B6-B9.
 */
class AiAppCheckException(cause: Throwable? = null) :
    Exception("App Check menolak panggilan AI", cause)
