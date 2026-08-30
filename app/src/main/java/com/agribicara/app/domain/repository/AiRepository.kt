package com.agribicara.app.domain.repository

import com.agribicara.app.core.common.NetworkResult

/**
 * Akses model bahasa.
 *
 * Kontrak (lihat [NetworkResult]): implementasi TIDAK PERNAH melempar
 * exception melewati batas ini kecuali pembatalan coroutine. Setiap kegagalan
 * menjadi [NetworkResult.Error] dengan pesan Bahasa Indonesia sederhana.
 *
 * Sengaja hanya menerima dan mengembalikan String: penyusunan prompt adalah
 * tugas `data/ai/PromptBuilder`, dan orkestrasi cuaca/cache/riwayat adalah
 * tugas `AskAgriUseCase`. Batas sempit ini yang membuat penggantian penyedia
 * AI di kemudian hari tidak menyentuh apa pun selain implementasinya.
 */
interface AiRepository {

    /**
     * Mengirim [prompt] yang sudah jadi dan mengembalikan teks jawaban.
     *
     * Jawaban kosong atau yang diblokir filter keamanan diperlakukan sebagai
     * [NetworkResult.Error], bukan Success bernilai kosong.
     */
    suspend fun ask(prompt: String): NetworkResult<String>
}
