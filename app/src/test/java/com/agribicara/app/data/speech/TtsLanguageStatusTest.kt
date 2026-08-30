package com.agribicara.app.data.speech

import android.speech.tts.TextToSpeech
import com.agribicara.app.domain.model.TtsStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `setLanguage` mengembalikan kode NEGATIF bila bahasa tidak bisa dipakai.
 * Salah membaca tanda di sini membuat aplikasi memanggil `speak()` pada engine
 * yang tidak punya suara Indonesia — hasilnya diam total tanpa error apa pun,
 * kegagalan yang paling sulit dilacak.
 */
class TtsLanguageStatusTest {

    @Test
    fun `data bahasa belum diunduh dibedakan dari bahasa tidak didukung`() {
        assertEquals(
            TtsStatus.LANGUAGE_MISSING_DATA,
            AndroidTextToSpeechRepository.languageStatusOf(TextToSpeech.LANG_MISSING_DATA),
        )
        assertEquals(
            TtsStatus.LANGUAGE_NOT_SUPPORTED,
            AndroidTextToSpeechRepository.languageStatusOf(TextToSpeech.LANG_NOT_SUPPORTED),
        )
    }

    @Test
    fun `tiga tingkat ketersediaan resmi semuanya dianggap siap`() {
        // LANG_AVAILABLE(0), LANG_COUNTRY_AVAILABLE(1), LANG_COUNTRY_VAR_AVAILABLE(2)
        // sama-sama berarti bisa dipakai; membedakannya tidak ada gunanya bagi petani.
        assertEquals(
            TtsStatus.READY,
            AndroidTextToSpeechRepository.languageStatusOf(TextToSpeech.LANG_AVAILABLE),
        )
        assertEquals(
            TtsStatus.READY,
            AndroidTextToSpeechRepository.languageStatusOf(TextToSpeech.LANG_COUNTRY_AVAILABLE),
        )
        assertEquals(
            TtsStatus.READY,
            AndroidTextToSpeechRepository.languageStatusOf(TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE),
        )
    }

    @Test
    fun `kode negatif tak terdokumentasi dianggap tidak didukung bukan siap`() {
        // Gagal-aman: menganggapnya siap akan menghasilkan diam tanpa penjelasan.
        assertEquals(TtsStatus.LANGUAGE_NOT_SUPPORTED, statusOf(-99))
        assertEquals(TtsStatus.LANGUAGE_NOT_SUPPORTED, statusOf(-3))
    }

    private fun statusOf(code: Int) = AndroidTextToSpeechRepository.languageStatusOf(code)
}
