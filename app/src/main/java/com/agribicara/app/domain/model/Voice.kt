package com.agribicara.app.domain.model

/**
 * Kejadian yang dipancarkan selama satu sesi mendengarkan.
 *
 * Sengaja memodelkan SIKLUS HIDUP sesi, bukan sekadar hasil akhirnya, karena
 * petani butuh umpan balik di setiap tahap: tanpa [Ready] dan
 * [BeginningOfSpeech], layar diam beberapa detik dan orang mengira aplikasinya
 * hang lalu menekan tombol berkali-kali.
 *
 * [Failed] adalah kejadian biasa, bukan exception. Kontraknya sama dengan
 * [com.agribicara.app.core.common.NetworkResult]: kegagalan tidak pernah
 * dilempar melewati batas repository.
 */
sealed interface SpeechEvent {

    /** Recognizer siap, mikrofon sudah terbuka. Saat inilah user boleh bicara. */
    data object Ready : SpeechEvent

    /** Suara terdeteksi masuk. */
    data object BeginningOfSpeech : SpeechEvent

    /**
     * Perubahan volume, 0f..1f setelah dinormalisasi.
     *
     * Dipakai untuk animasi pulsa. Nilai mentah dari Android berupa dBFS
     * kira-kira -2..10, jadi normalisasi dilakukan di layer data agar UI tidak
     * perlu tahu satuan aslinya.
     */
    data class RmsChanged(val level: Float) : SpeechEvent

    /** Tebakan sementara; boleh berubah sampai [FinalResult] datang. */
    data class PartialResult(val text: String) : SpeechEvent

    /** Hasil akhir. Sesi berakhir setelah ini. */
    data class FinalResult(val text: String) : SpeechEvent

    /**
     * Sesi gagal.
     *
     * [message] sudah berupa Bahasa Indonesia sederhana yang layak tampil ke
     * petani; [isRecoverable] menentukan apakah tombol "Coba lagi" ditawarkan
     * atau pengguna langsung diarahkan ke input teks.
     */
    data class Failed(
        val message: String,
        val isRecoverable: Boolean,
    ) : SpeechEvent
}

/**
 * Kesiapan mesin suara di perangkat ini.
 *
 * Dihitung sekali saat layar suara dibuka. Kalau [isSpeechRecognitionAvailable]
 * false, layar langsung membuka jalur teks tanpa pernah meminta izin mikrofon
 * — meminta izin untuk kemampuan yang tidak ada hanya membuang friksi.
 */
data class VoiceAvailability(
    val isSpeechRecognitionAvailable: Boolean,
    val isTextToSpeechReady: Boolean,
)

/**
 * Hasil penyiapan TextToSpeech.
 *
 * Dibedakan dari sekadar boolean karena tiga keadaan gagal ini menuntut pesan
 * yang berbeda ke pengguna: engine tidak ada sama sekali, engine ada tapi
 * suara Indonesia belum diunduh, atau data bahasanya rusak.
 */
enum class TtsStatus {
    /** Siap membacakan Bahasa Indonesia. */
    READY,

    /** Engine hidup, tapi Bahasa Indonesia tidak didukung. */
    LANGUAGE_NOT_SUPPORTED,

    /** Engine hidup, Bahasa Indonesia didukung tapi datanya belum diunduh. */
    LANGUAGE_MISSING_DATA,

    /** Engine gagal init, atau tidak ada engine sama sekali. */
    ENGINE_UNAVAILABLE,
}
