package com.agribicara.app.domain.repository

import com.agribicara.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Riwayat percakapan yang tersimpan lokal.
 *
 * Kontrak: implementasi TIDAK PERNAH melempar exception melewati batas ini.
 * Kegagalan menulis riwayat tidak boleh menggagalkan jawaban yang sudah
 * berhasil didapat — riwayat adalah kenyamanan, jawabannya yang utama.
 */
interface ChatHistoryRepository {

    /** Menyimpan satu baris. Kegagalan ditelan dan dicatat, bukan dilempar. */
    suspend fun append(message: ChatMessage)

    /**
     * Mencari jawaban tersimpan untuk pertanyaan yang setara.
     *
     * @param questionKey pertanyaan yang sudah dinormalisasi
     * @param regionCode wilayah saat bertanya; jawaban dari wilayah lain
     *   TIDAK boleh dipakai ulang
     * @param notBefore batas epoch millis terlama yang masih dianggap segar
     * @return teks jawaban, atau null bila tidak ada yang cocok dan segar
     */
    suspend fun findCachedAnswer(
        questionKey: String,
        regionCode: String?,
        notBefore: Long,
    ): String?

    /** Riwayat terbaru lebih dulu. UI-nya menyusul di fase berikutnya. */
    fun observeHistory(limit: Int): Flow<List<ChatMessage>>
}
