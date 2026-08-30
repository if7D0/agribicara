package com.agribicara.app.domain.repository

import com.agribicara.app.domain.model.SpeechEvent
import kotlinx.coroutines.flow.Flow

/**
 * Pengenalan suara menjadi teks.
 *
 * Kontrak (sama dengan [com.agribicara.app.domain.repository.WeatherRepository]):
 * implementasi TIDAK PERNAH melempar exception melewati batas ini. Setiap
 * kegagalan menjadi [SpeechEvent.Failed] dengan pesan Bahasa Indonesia
 * sederhana.
 *
 * Batas ini ada supaya ViewModel bisa diuji sebagai unit test JVM murni tanpa
 * Robolectric — persis alasan yang sama dengan repository cuaca di Fase 2.
 */
interface SpeechRecognizerRepository {

    /**
     * Apakah perangkat ini punya layanan pengenalan suara.
     *
     * PENTING: bernilai false pada emulator AOSP dan perangkat tanpa Google
     * services. Itu keadaan yang sah, bukan error — pemanggil harus menyiapkan
     * jalur teks, bukan menampilkan pesan gagal.
     */
    fun isAvailable(): Boolean

    /**
     * Satu sesi mendengarkan.
     *
     * Flow berakhir sendiri setelah [SpeechEvent.FinalResult] atau
     * [SpeechEvent.Failed]. Membatalkan collect akan menghentikan recognizer
     * dan melepaskan mikrofon.
     */
    fun listen(languageTag: String): Flow<SpeechEvent>
}
