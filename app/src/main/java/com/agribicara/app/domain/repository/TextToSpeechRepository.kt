package com.agribicara.app.domain.repository

import com.agribicara.app.domain.model.TtsStatus

/**
 * Pembacaan teks menjadi suara.
 *
 * Kontrak: tidak pernah melempar exception melewati batas ini. TTS yang gagal
 * TIDAK BOLEH mematikan layar suara — petani masih bisa membaca teksnya, jadi
 * kegagalan di sini selalu berupa penurunan kualitas, bukan jalan buntu.
 */
interface TextToSpeechRepository {

    /**
     * Menyalakan engine dan memilih Bahasa Indonesia.
     *
     * Aman dipanggil berkali-kali: pemanggilan setelah yang pertama
     * mengembalikan status yang sama tanpa menyalakan engine kedua.
     */
    suspend fun prepare(): TtsStatus

    /**
     * Membacakan [text].
     *
     * Ucapan sebelumnya dihentikan (QUEUE_FLUSH), bukan diantrikan — pertanyaan
     * terbaru selalu yang paling relevan bagi petani.
     */
    suspend fun speak(text: String)

    /** Menghentikan ucapan yang sedang berjalan. */
    fun stop()

    /** Melepaskan engine. Wajib dipanggil saat layar suara ditutup. */
    fun shutdown()
}
