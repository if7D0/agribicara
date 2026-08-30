package com.agribicara.app.domain.ai

import com.agribicara.app.core.common.Constants
import com.agribicara.app.domain.repository.ChatHistoryRepository
import java.time.Clock
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menemukan jawaban tersimpan untuk pertanyaan yang setara.
 *
 * Gunanya muncul justru saat keadaan paling buruk: sinyal hilang di tengah
 * sawah, Gemini tidak bisa dihubungi, dan petani menanyakan hal yang sama
 * dengan yang pernah ia tanyakan pagi tadi.
 */
@Singleton
class AnswerCache @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository,
    private val clock: Clock,
) {

    /**
     * Mencari jawaban yang masih layak dipakai.
     *
     * @return teks jawaban, atau null bila tidak ada yang cocok, wilayahnya
     *   berbeda, atau sudah terlalu tua.
     */
    suspend fun find(question: String, regionCode: String?): String? {
        val key = keyOf(question)
        if (key.isEmpty()) return null

        val notBefore = clock.millis() - TimeUnit.HOURS.toMillis(Constants.ANSWER_CACHE_HOURS)
        return chatHistoryRepository.findCachedAnswer(key, regionCode, notBefore)
    }

    companion object {
        /**
         * Menormalkan pertanyaan menjadi kunci pencocokan.
         *
         * Huruf kecil, spasi dirapikan, tanda baca dibuang — sehingga
         * "Kapan memupuk padi?" dan "kapan  memupuk padi" dianggap satu
         * pertanyaan. Ini penting karena STT jarang menghasilkan tanda baca
         * yang konsisten, sementara ketikan hampir selalu punya.
         *
         * Ditulis sebagai loop, bukan Regex, agar bebas escape sequence dan
         * tidak mengompilasi pola pada setiap panggilan.
         */
        fun keyOf(question: String): String {
            val out = StringBuilder(question.length)
            var previousWasSpace = false

            for (character in question.trim()) {
                when {
                    character.isLetterOrDigit() -> {
                        out.append(character.lowercaseChar())
                        previousWasSpace = false
                    }

                    character.isWhitespace() -> {
                        if (!previousWasSpace && out.isNotEmpty()) out.append(' ')
                        previousWasSpace = true
                    }

                    // Tanda baca dibuang tanpa menjadi pemisah: "padi?" dan
                    // "padi" harus menghasilkan kunci yang sama persis.
                    else -> Unit
                }
            }

            return out.toString().trim()
        }
    }
}
