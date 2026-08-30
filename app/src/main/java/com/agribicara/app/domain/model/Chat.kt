package com.agribicara.app.domain.model

/**
 * Model domain percakapan (Fase 5).
 *
 * Sama seperti [Forecast], paket `domain/` tidak boleh mengimpor apa pun dari
 * `data/` — entity Room dipetakan ke model ini di data layer.
 */

/** Siapa yang menulis satu baris percakapan. */
enum class ChatRole {
    /** Pertanyaan petani, baik dari suara maupun ketikan. */
    USER,

    /** Jawaban dari AI. */
    ASSISTANT,
}

/**
 * Satu baris riwayat.
 *
 * @param questionKey pertanyaan yang sudah dinormalisasi (lihat
 *   `data/ai/AnswerCache`). Diisi untuk KEDUA peran: baris USER memakainya
 *   sebagai identitas pertanyaan, baris ASSISTANT memakainya sebagai kunci
 *   pencarian cache. Tanpa kunci di baris jawaban, cache harus melakukan join
 *   ke baris pertanyaan hanya untuk mencocokkan teks.
 * @param regionCode wilayah saat pertanyaan diajukan, boleh null bila petani
 *   melewati region picker. Bagian dari kunci cache: jawaban ber-grounding
 *   cuaca desa lain tidak boleh dipakai ulang.
 */
data class ChatMessage(
    val id: Long = 0,
    val role: ChatRole,
    val text: String,
    val questionKey: String,
    val regionCode: String?,
    val createdAt: Long,
)

/**
 * Jawaban yang sampai ke layar, beserta asalnya.
 *
 * [source] dibedakan karena berpengaruh ke UI: jawaban dari cache diberi
 * catatan agar petani tahu itu jawaban lama, bukan hasil tanya barusan.
 */
data class AiAnswer(
    val text: String,
    val source: AnswerSource,
)

enum class AnswerSource {
    /** Baru saja dijawab Gemini. */
    AI,

    /** Diambil dari jawaban tersimpan karena AI tidak bisa dihubungi. */
    CACHE,
}
