package com.agribicara.app.domain.usecase

import com.agribicara.app.core.common.Constants
import com.agribicara.app.core.common.NetworkResult
import com.agribicara.app.domain.ai.AnswerCache
import com.agribicara.app.domain.ai.PromptBuilder
import com.agribicara.app.domain.model.AiAnswer
import com.agribicara.app.domain.model.AnswerSource
import com.agribicara.app.domain.model.ChatMessage
import com.agribicara.app.domain.model.ChatRole
import com.agribicara.app.domain.model.Forecast
import com.agribicara.app.domain.model.Region
import com.agribicara.app.domain.repository.AiRepository
import com.agribicara.app.domain.repository.ChatHistoryRepository
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Menjawab pertanyaan petani, di-grounding cuaca wilayahnya.
 *
 * Tangga degradasi, dari yang paling diinginkan ke yang paling buruk:
 *
 * ```
 * cuaca ada?  --tidak--> tetap bertanya, prompt MENYATAKAN cuaca tak tersedia
 *   |
 *  ya
 *   v
 * Gemini  --sukses--> simpan riwayat, tampilkan
 *   |
 *   +--gagal--> jawaban tersimpan untuk pertanyaan setara
 *                  |
 *                  +--tidak ada--> Error dengan pesan yang bisa ditindaklanjuti
 * ```
 *
 * Gagalnya cuaca TIDAK memblokir pertanyaan: banyak pertanyaan pertanian
 * (jarak tanam, tanda hama) tidak bergantung cuaca sama sekali.
 */
class AskAgriUseCase @Inject constructor(
    private val aiRepository: AiRepository,
    private val chatHistoryRepository: ChatHistoryRepository,
    private val answerCache: AnswerCache,
    private val getForecast: GetForecastUseCase,
    private val observeSelectedRegion: ObserveSelectedRegionUseCase,
    private val clock: Clock,
) {

    suspend operator fun invoke(question: String): NetworkResult<AiAnswer> {
        val questionKey = AnswerCache.keyOf(question)
        if (questionKey.isEmpty()) {
            // Pertanyaan kosong atau hanya tanda baca. Ditolak di sini, bukan
            // dikirim ke Gemini: memanggil model untuk string kosong membakar
            // kuota dan menghasilkan jawaban mengarang.
            return NetworkResult.Error(EMPTY_QUESTION)
        }

        val region = currentRegion()
        val forecast = forecastFor(region)
        val today = LocalDate.now(clock.withZone(ZoneId.of(Constants.DEFAULT_TIMEZONE)))

        chatHistoryRepository.append(
            ChatMessage(
                role = ChatRole.USER,
                text = question.trim(),
                questionKey = questionKey,
                regionCode = region?.code,
                createdAt = clock.millis(),
            ),
        )

        val prompt = PromptBuilder.build(question, forecast, today)

        return when (val result = aiRepository.ask(prompt)) {
            is NetworkResult.Success -> {
                chatHistoryRepository.append(
                    ChatMessage(
                        role = ChatRole.ASSISTANT,
                        text = result.data,
                        questionKey = questionKey,
                        regionCode = region?.code,
                        createdAt = clock.millis(),
                    ),
                )
                NetworkResult.Success(AiAnswer(result.data, AnswerSource.AI))
            }

            is NetworkResult.Error -> fallbackToCache(question, region?.code, result)

            NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    /**
     * Jawaban tersimpan hanya dipakai setelah AI benar-benar gagal.
     *
     * Bila juga tidak ada, pesan kegagalan ASLI dari repository yang
     * diteruskan — bukan pesan generik. Repository sudah membedakan "tidak ada
     * internet" dari "layanan bermasalah", dan perbedaan itu menentukan apa
     * yang bisa dilakukan petani.
     */
    private suspend fun fallbackToCache(
        question: String,
        regionCode: String?,
        failure: NetworkResult.Error,
    ): NetworkResult<AiAnswer> {
        val cached = answerCache.find(question, regionCode)
        return if (cached != null) {
            Timber.i("Gemini gagal, memakai jawaban tersimpan")
            NetworkResult.Success(AiAnswer(cached, AnswerSource.CACHE))
        } else {
            failure
        }
    }

    /** Wilayah belum dipilih adalah keadaan SAH — petani boleh melewati picker. */
    private suspend fun currentRegion(): Region? = try {
        observeSelectedRegion().first()
    } catch (e: Exception) {
        Timber.w(e, "Gagal membaca wilayah terpilih")
        null
    }

    private suspend fun forecastFor(region: Region?): Forecast? {
        if (region == null) return null
        return when (val result = getForecast(region.code, region.name)) {
            is NetworkResult.Success -> result.data
            else -> {
                // Bukan penghalang: prompt akan menyatakan cuaca tidak
                // tersedia, dan pertanyaan tetap dijawab.
                Timber.w("Cuaca tidak tersedia untuk grounding, tetap bertanya")
                null
            }
        }
    }

    private companion object {
        /**
         * Satu-satunya pesan yang ditulis inline di layer domain.
         *
         * Layer ini tidak punya Context, dan menyuntikkan Context ke use case
         * hanya demi satu string akan mencemari batas domain/data. Pesan
         * kegagalan lainnya semuanya berasal dari strings.xml lewat
         * repository.
         */
        const val EMPTY_QUESTION = "Pertanyaannya masih kosong."
    }
}

/** Riwayat percakapan terbaru. UI-nya menyusul di fase berikutnya. */
class ObserveChatHistoryUseCase @Inject constructor(
    private val chatHistoryRepository: ChatHistoryRepository,
) {
    operator fun invoke(limit: Int = Constants.CHAT_HISTORY_LIMIT): Flow<List<ChatMessage>> =
        chatHistoryRepository.observeHistory(limit)
}
