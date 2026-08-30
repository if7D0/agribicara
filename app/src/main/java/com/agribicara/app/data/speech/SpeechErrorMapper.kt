package com.agribicara.app.data.speech

import android.speech.SpeechRecognizer
import androidx.annotation.StringRes
import com.agribicara.app.R

/**
 * Satu kegagalan pengenalan suara yang sudah siap ditampilkan.
 *
 * Membawa [messageRes], bukan String jadi, supaya mapper ini tetap murni dan
 * bisa diuji sebagai unit test JVM tanpa Context — repository-lah yang
 * meresolusinya lewat `context.getString`, sama seperti pola Fase 2.
 */
data class SpeechFailure(
    @param:StringRes val messageRes: Int,
    /**
     * Apakah menekan "Coba lagi" masuk akal.
     *
     * False berarti mengulang pasti gagal lagi dengan cara yang sama (izin
     * belum ada, bahasa tidak terpasang) sehingga pengguna harus dibawa ke
     * jalur lain — alur izin atau input teks — bukan disuruh mengulang.
     */
    val isRecoverable: Boolean,
)

/**
 * Menerjemahkan kode error [SpeechRecognizer] menjadi pesan Bahasa Indonesia.
 *
 * Kenapa dipetakan satu per satu dan tidak digeneralisasi: bagi petani,
 * "tidak ada sinyal" dan "suara Anda tidak terdengar" menuntut tindakan yang
 * sama sekali berbeda. Pesan umum untuk semuanya akan membuat orang menyerah
 * padahal masalahnya sepele.
 */
object SpeechErrorMapper {

    fun map(errorCode: Int): SpeechFailure = when (errorCode) {
        SpeechRecognizer.ERROR_NETWORK ->
            SpeechFailure(R.string.voice_error_network, isRecoverable = true)

        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            SpeechFailure(R.string.voice_error_network_timeout, isRecoverable = true)

        SpeechRecognizer.ERROR_AUDIO ->
            SpeechFailure(R.string.voice_error_audio, isRecoverable = true)

        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
        ->
            SpeechFailure(R.string.voice_error_server, isRecoverable = true)

        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            SpeechFailure(R.string.voice_error_no_speech, isRecoverable = true)

        SpeechRecognizer.ERROR_NO_MATCH ->
            SpeechFailure(R.string.voice_error_no_match, isRecoverable = true)

        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            SpeechFailure(R.string.voice_error_busy, isRecoverable = true)

        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS ->
            SpeechFailure(R.string.voice_error_too_many_requests, isRecoverable = true)

        // Mengulang tanpa izin hanya menghasilkan error yang sama; pengguna
        // harus dibawa ke alur izin.
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            SpeechFailure(R.string.voice_error_permission, isRecoverable = false)

        // Mencoba lagi tidak akan pernah memasang Bahasa Indonesia di
        // perangkat ini — arahkan ke jalur teks.
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        ->
            SpeechFailure(R.string.voice_error_language, isRecoverable = false)

        // Kesalahan pemakaian API dari sisi kita; mengulang tidak memperbaiki.
        SpeechRecognizer.ERROR_CLIENT ->
            SpeechFailure(R.string.voice_error_client, isRecoverable = false)

        // Dua kode ini menyangkut pemeriksaan dukungan dan unduhan model, yang
        // tidak kita pakai. Bila muncul, perlakukan seperti gangguan sesaat.
        SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT,
        SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS,
        ->
            SpeechFailure(R.string.voice_error_server, isRecoverable = true)

        else -> SpeechFailure(R.string.voice_error_generic, isRecoverable = true)
    }
}
