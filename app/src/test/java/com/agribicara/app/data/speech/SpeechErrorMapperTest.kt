package com.agribicara.app.data.speech

import android.speech.SpeechRecognizer
import com.agribicara.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mapper ini logika murni tanpa panggilan Android, jadi bisa diuji sebagai
 * unit test JVM biasa.
 *
 * Konstanta ERROR_* aman dipakai di sini: semuanya `static final int` sehingga
 * di-inline compiler dan tidak pernah menyentuh android.jar saat runtime test.
 */
class SpeechErrorMapperTest {

    @Test
    fun `error jaringan memberi pesan koneksi dan boleh dicoba lagi`() {
        val failure = SpeechErrorMapper.map(SpeechRecognizer.ERROR_NETWORK)

        assertEquals(R.string.voice_error_network, failure.messageRes)
        assertTrue(failure.isRecoverable)
    }

    @Test
    fun `timeout jaringan dibedakan dari tidak ada jaringan`() {
        val timeout = SpeechErrorMapper.map(SpeechRecognizer.ERROR_NETWORK_TIMEOUT)
        val offline = SpeechErrorMapper.map(SpeechRecognizer.ERROR_NETWORK)

        assertEquals(R.string.voice_error_network_timeout, timeout.messageRes)
        // Pesannya harus berbeda: "lambat" dan "tidak tersambung" menuntut
        // tindakan berbeda dari petani.
        assertTrue(timeout.messageRes != offline.messageRes)
    }

    @Test
    fun `tidak ada suara terdengar boleh dicoba lagi`() {
        val failure = SpeechErrorMapper.map(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)

        assertEquals(R.string.voice_error_no_speech, failure.messageRes)
        assertTrue(failure.isRecoverable)
    }

    @Test
    fun `ucapan tidak dikenali boleh dicoba lagi`() {
        val failure = SpeechErrorMapper.map(SpeechRecognizer.ERROR_NO_MATCH)

        assertEquals(R.string.voice_error_no_match, failure.messageRes)
        assertTrue(failure.isRecoverable)
    }

    @Test
    fun `masalah audio boleh dicoba lagi`() {
        val failure = SpeechErrorMapper.map(SpeechRecognizer.ERROR_AUDIO)

        assertEquals(R.string.voice_error_audio, failure.messageRes)
        assertTrue(failure.isRecoverable)
    }

    @Test
    fun `recognizer sibuk boleh dicoba lagi`() {
        val failure = SpeechErrorMapper.map(SpeechRecognizer.ERROR_RECOGNIZER_BUSY)

        assertEquals(R.string.voice_error_busy, failure.messageRes)
        assertTrue(failure.isRecoverable)
    }

    @Test
    fun `error server boleh dicoba lagi`() {
        assertTrue(SpeechErrorMapper.map(SpeechRecognizer.ERROR_SERVER).isRecoverable)
        assertTrue(SpeechErrorMapper.map(SpeechRecognizer.ERROR_SERVER_DISCONNECTED).isRecoverable)
    }

    @Test
    fun `terlalu banyak permintaan boleh dicoba lagi`() {
        val failure = SpeechErrorMapper.map(SpeechRecognizer.ERROR_TOO_MANY_REQUESTS)

        assertEquals(R.string.voice_error_too_many_requests, failure.messageRes)
        assertTrue(failure.isRecoverable)
    }

    @Test
    fun `izin kurang TIDAK boleh dicoba lagi karena butuh alur izin`() {
        val failure = SpeechErrorMapper.map(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)

        assertEquals(R.string.voice_error_permission, failure.messageRes)
        // Mengulang startListening tanpa izin hanya menghasilkan error yang
        // sama; pengguna harus dibawa ke alur izin, bukan disuruh coba lagi.
        assertFalse(failure.isRecoverable)
    }

    @Test
    fun `bahasa tidak didukung TIDAK boleh dicoba lagi dan mengarah ke jalur teks`() {
        val notSupported = SpeechErrorMapper.map(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)
        val unavailable = SpeechErrorMapper.map(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)

        assertEquals(R.string.voice_error_language, notSupported.messageRes)
        assertEquals(R.string.voice_error_language, unavailable.messageRes)
        // Mencoba lagi tidak akan pernah memasang Bahasa Indonesia.
        assertFalse(notSupported.isRecoverable)
        assertFalse(unavailable.isRecoverable)
    }

    @Test
    fun `error klien TIDAK boleh dicoba lagi`() {
        assertFalse(SpeechErrorMapper.map(SpeechRecognizer.ERROR_CLIENT).isRecoverable)
    }

    @Test
    fun `kode tak dikenal jatuh ke pesan umum tanpa melempar exception`() {
        val failure = SpeechErrorMapper.map(9999)

        assertEquals(R.string.voice_error_generic, failure.messageRes)
        assertTrue(failure.isRecoverable)
    }

    @Test
    fun `semua kode error resmi punya pemetaan sendiri bukan pesan umum`() {
        // Menjaga agar konstanta yang ditambahkan Android di masa depan tidak
        // diam-diam lolos sebagai "kode tak dikenal" tanpa kita sadari.
        val officialCodes = 1..15
        val unmapped = officialCodes.filter {
            SpeechErrorMapper.map(it).messageRes == R.string.voice_error_generic
        }

        assertEquals(emptyList<Int>(), unmapped)
    }
}
